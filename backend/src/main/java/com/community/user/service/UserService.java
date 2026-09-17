package com.community.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.JwtService;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.oauth.service.OAuthService;
import com.community.user.dto.LoginRequest;
import com.community.user.dto.MeResponse;
import com.community.user.dto.OAuthBindingsResponse;
import com.community.user.dto.RegisterRequest;
import com.community.user.dto.TokenResponse;
import com.community.user.dto.UpdateMeRequest;
import com.community.user.dto.UpdatePasswordRequest;
import com.community.user.dto.UserResponse;
import com.community.user.entity.User;
import com.community.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户注册 / 登录 / 令牌刷新 / 资料查询与更新。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserOauthBindingMapper bindingMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserMapper userMapper, UserOauthBindingMapper bindingMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** 注册并直接登录（返回令牌对）。用户名/邮箱重复返回 409；并发下由唯一索引兜底。 */
    public TokenResponse register(RegisterRequest request) {
        String username = request.username();
        String email = request.email();
        if (countByUsernameOrEmail(username, email) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已被占用");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(username);
        user.setRole("USER");
        user.setStatus(0);
        user.setTokenVersion(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // P2-4：并发注册绕过 count 检查时由唯一索引兜底，转统一 409 而非 500
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已被占用");
        }
        return issueTokens(user);
    }

    /** 用户名密码登录；凭据错误统一返回 401（不区分账号不存在与密码错误）。 */
    public TokenResponse login(LoginRequest request) {
        User user = selectByUsername(request.username());
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁");
        }
        return issueTokens(user);
    }

    /**
     * 用刷新令牌换新令牌对；令牌非法/过期/类型不符/版本不符返回 401。
     * P2-3：签发前查一次用户状态——封禁用户即使 refresh 令牌仍在有效期也不能续期。
     * T-PROFILE：改密后 token_version 递增，旧 refresh 的 ver 与库不一致则拒绝。
     */
    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的刷新令牌");
        }
        String username = claims.getSubject();
        User user = selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁");
        }
        if (jwtService.tokenVersion(claims) != normalizeVersion(user.getTokenVersion())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return issueTokens(user);
    }

    /** 按用户名查询当前登录用户资料；不存在返回 404。 */
    public UserResponse me(String username) {
        User user = requireUser(username);
        return toResponse(user);
    }

    /** 当前用户完整资料（含 hasPassword / oauthBindings，供设置页）。 */
    public MeResponse meDetail(String username) {
        return toMeResponse(requireUser(username));
    }

    /**
     * 更新当前用户昵称/简介/头像 URL（null 字段不改）。
     * /me 仅作用于 JWT 主体本人，无跨用户路径（隔离靠令牌，非他人 id）。
     */
    public MeResponse updateMe(String username, UpdateMeRequest request) {
        User user = requireUser(username);
        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (request.bio() != null) {
            String bio = request.bio().trim();
            user.setBio(bio.isEmpty() ? null : bio);
        }
        if (request.avatarUrl() != null) {
            String avatar = request.avatarUrl().trim();
            user.setAvatarUrl(avatar.isEmpty() ? null : avatar);
        }
        userMapper.updateById(user);
        return toMeResponse(user);
    }

    /**
     * 设置或修改密码。
     * <ul>
     *   <li>已有 password_hash：必须校验 oldPassword</li>
     *   <li>OAuth 无密用户首次设置：免验旧密码</li>
     *   <li>成功后 token_version++：旧 refresh 立即失效；旧 access 最长存活 access-ttl</li>
     * </ul>
     */
    @Transactional
    public void changePassword(String username, UpdatePasswordRequest request) {
        User user = requireUser(username);
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        if (hasPassword) {
            if (request.oldPassword() == null || request.oldPassword().isBlank()
                    || !passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "原密码错误");
            }
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(normalizeVersion(user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    /** 为已加载的用户签发令牌对（供 OAuth 等模块复用，带 token_version）。 */
    public TokenResponse issueTokens(User user) {
        int ver = normalizeVersion(user.getTokenVersion());
        return new TokenResponse(
                jwtService.generateAccess(user.getUsername(), ver),
                jwtService.generateRefresh(user.getUsername(), ver));
    }

    private MeResponse toMeResponse(User user) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        boolean github = bindingMapper.selectCount(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getUserId, user.getId())
                .eq(UserOauthBinding::getProvider, OAuthService.GITHUB)) > 0;
        boolean gitee = bindingMapper.selectCount(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getUserId, user.getId())
                .eq(UserOauthBinding::getProvider, OAuthService.GITEE)) > 0;
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getRole(),
                user.getCreatedAt(),
                hasPassword,
                new OAuthBindingsResponse(github, gitee));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getNickname(),
                user.getAvatarUrl(), user.getBio(), user.getRole(), user.getCreatedAt());
    }

    private User requireUser(String username) {
        User user = selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private static int normalizeVersion(Integer tokenVersion) {
        return tokenVersion == null ? 0 : tokenVersion;
    }

    private User selectByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private Long countByUsernameOrEmail(String username, String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, username).or().eq(User::getEmail, email)));
    }
}
