package com.community.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.JwtService;
import com.community.user.dto.LoginRequest;
import com.community.user.dto.RegisterRequest;
import com.community.user.dto.TokenResponse;
import com.community.user.dto.UserResponse;
import com.community.user.entity.User;
import com.community.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户注册 / 登录 / 令牌刷新 / 资料查询。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
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
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // P2-4：并发注册绕过 count 检查时由唯一索引兜底，转统一 409 而非 500
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已被占用");
        }
        return issueTokens(username);
    }

    /** 用户名密码登录；凭据错误统一返回 401（不区分账号不存在与密码错误）。 */
    public TokenResponse login(LoginRequest request) {
        User user = selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁");
        }
        return issueTokens(user.getUsername());
    }

    /** 用刷新令牌换新令牌对；令牌非法/过期/类型不符返回 401。 */
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
        return issueTokens(claims.getSubject());
    }

    /** 按用户名查询当前登录用户资料；不存在返回 404。 */
    public UserResponse me(String username) {
        User user = selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getNickname(),
                user.getAvatarUrl(), user.getBio(), user.getRole(), user.getCreatedAt());
    }

    private TokenResponse issueTokens(String username) {
        return new TokenResponse(jwtService.generateAccess(username), jwtService.generateRefresh(username));
    }

    private User selectByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private Long countByUsernameOrEmail(String username, String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, username).or().eq(User::getEmail, email)));
    }
}
