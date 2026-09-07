package com.community.oauth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.AesGcmCipher;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.JwtService;
import com.community.oauth.dto.OAuthUserInfo;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.user.dto.RegisterRequest;
import com.community.user.dto.TokenResponse;
import com.community.user.dto.UserResponse;
import com.community.user.entity.User;
import com.community.user.mapper.UserMapper;
import com.community.user.service.UserService;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 双语义回调：
 * - 未登录（viewerUsername=null）：第三方登录——按 (provider, openid) 找绑定；
 *   命中→直接发令牌；未命中→邮箱冲突检查（保守：邮箱已被本地账号占用则 409 提示"用密码登录后绑定"，
 *   绝不自动合并），否则自动注册新账号并绑定。
 * - 已登录：绑定到当前账号；一个 (provider, openid) 只能绑一个账号，重复绑定 409；
 *   一个用户同一 provider 只保留一条绑定。
 * 红线：access token AES-GCM 加密后入库，refresh token 同样加密（Gitee refresh 视同主凭据）。
 */
@Service
public class OAuthService {

    public static final String GITHUB = "GITHUB";
    public static final String GITEE = "GITEE";

    private final UserOauthBindingMapper bindingMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final GitHubOAuthService gitHubOAuthService;
    private final GiteeOAuthService giteeOAuthService;
    private final AesGcmCipher cipher;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RepoSyncService repoSyncService;

    public OAuthService(UserOauthBindingMapper bindingMapper, UserMapper userMapper,
                        UserService userService, GitHubOAuthService gitHubOAuthService,
                        GiteeOAuthService giteeOAuthService, AesGcmCipher cipher,
                        JwtService jwtService, PasswordEncoder passwordEncoder,
                        RepoSyncService repoSyncService) {
        this.bindingMapper = bindingMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.gitHubOAuthService = gitHubOAuthService;
        this.giteeOAuthService = giteeOAuthService;
        this.cipher = cipher;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.repoSyncService = repoSyncService;
    }

    /** 回调入口：provider = GITHUB | GITEE。 */
    @Transactional
    public TokenResponse handleCallback(String provider, String code, String viewerUsername) {
        String normalized = normalizeProvider(provider);
        if (GITEE.equals(normalized)) {
            GiteeOAuthService.TokenExchangeResult exchange = giteeOAuthService.exchangeCode(code);
            OAuthUserInfo info = giteeOAuthService.fetchUserInfo(exchange.accessToken());
            return dispatch(normalized, info, exchange.accessToken(), exchange.refreshToken(),
                    exchange.expiresAt(), viewerUsername);
        }
        String accessToken = gitHubOAuthService.exchangeCode(code);
        OAuthUserInfo info = gitHubOAuthService.fetchUserInfo(accessToken);
        return dispatch(normalized, info, accessToken, null, null, viewerUsername);
    }

    /** 解绑（用户主动）。 */
    public void unbind(String provider, String username) {
        String normalized = normalizeProvider(provider);
        UserResponse me = userService.me(username);
        if (me.email() == null && hasNoOtherBinding(me.id(), normalized)) {
            // 解绑后账号将既无密码（第三方注册）也无其他绑定，会导致无法登录
            boolean hasPassword = hasPassword(me.id());
            if (!hasPassword) {
                throw new BusinessException(ErrorCode.CONFLICT, "该账号未设置密码，解绑后将无法登录，请先设置密码");
            }
        }
        bindingMapper.delete(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getUserId, me.id())
                .eq(UserOauthBinding::getProvider, normalized));
    }

    private TokenResponse dispatch(String provider, OAuthUserInfo info, String accessToken,
                                   String refreshToken, LocalDateTime expiresAt, String viewerUsername) {
        if (viewerUsername != null) {
            return bindToCurrentUser(provider, info, accessToken, refreshToken, expiresAt, viewerUsername);
        }
        return loginOrRegister(provider, info, accessToken, refreshToken, expiresAt);
    }

    /** 已登录：绑定到当前账号。 */
    private TokenResponse bindToCurrentUser(String provider, OAuthUserInfo info, String accessToken,
                                            String refreshToken, LocalDateTime expiresAt, String viewerUsername) {
        UserResponse me = userService.me(viewerUsername);
        // 同一 openid 已绑定其他账号 → 409
        UserOauthBinding existingOpenid = selectByProviderOpenid(provider, info.openid());
        if (existingOpenid != null && !existingOpenid.getUserId().equals(me.id())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该" + provider + "账号已绑定其他用户");
        }
        saveOrUpdateBinding(me.id(), provider, info, accessToken, refreshToken, expiresAt);
        repoSyncService.syncRepos(me.id(), provider);
        return new TokenResponse(jwtService.generateAccess(me.username()), jwtService.generateRefresh(me.username()));
    }

    /** 未登录：找绑定→命中登录；未命中→邮箱冲突检查→自动注册+绑定。 */
    private TokenResponse loginOrRegister(String provider, OAuthUserInfo info, String accessToken,
                                          String refreshToken, LocalDateTime expiresAt) {
        UserOauthBinding binding = selectByProviderOpenid(provider, info.openid());
        if (binding != null) {
            saveOrUpdateBinding(binding.getUserId(), provider, info, accessToken, refreshToken, expiresAt);
            repoSyncService.syncRepos(binding.getUserId(), provider);
            User user = userMapper.selectById(binding.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "绑定账号不存在");
            }
            if (user.getStatus() != null && user.getStatus() != 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁");
            }
            return new TokenResponse(jwtService.generateAccess(user.getUsername()),
                    jwtService.generateRefresh(user.getUsername()));
        }

        // 邮箱冲突（保守语义，PM 拍板）：邮箱已被本地账号占用 → 提示走密码登录后绑定，不自动合并
        if (info.email() != null) {
            User emailOwner = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, info.email()));
            if (emailOwner != null) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "该邮箱已注册，请使用密码登录后在设置中绑定" + provider + "账号");
            }
        }

        // 自动注册：用户名取平台 login（冲突时加后缀），密码为空（第三方登录用户），邮箱用平台邮箱
        String baseUsername = info.username() != null ? info.username() : ("user" + info.openid());
        String username = baseUsername;
        int suffix = 0;
        while (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0) {
            suffix++;
            username = baseUsername + "_" + suffix;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(info.email());
        user.setPasswordHash(null);
        user.setNickname(info.nickname() != null ? info.nickname() : username);
        user.setAvatarUrl(info.avatarUrl());
        user.setRole("USER");
        user.setStatus(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已被占用");
        }
        saveOrUpdateBinding(user.getId(), provider, info, accessToken, refreshToken, expiresAt);
        repoSyncService.syncRepos(user.getId(), provider);
        return new TokenResponse(jwtService.generateAccess(user.getUsername()),
                jwtService.generateRefresh(user.getUsername()));
    }

    private void saveOrUpdateBinding(long userId, String provider, OAuthUserInfo info,
                                     String accessToken, String refreshToken, LocalDateTime expiresAt) {
        UserOauthBinding binding = selectByProviderOpenid(provider, info.openid());
        if (binding == null) {
            binding = new UserOauthBinding();
            binding.setUserId(userId);
            binding.setProvider(provider);
            binding.setOpenid(info.openid());
        }
        binding.setAccessTokenEnc(cipher.encrypt(accessToken));
        // refresh token 同样加密保存（Gitee 刷新凭据，视同主凭据）
        binding.setRefreshToken(refreshToken == null ? null : cipher.encrypt(refreshToken));
        binding.setExpiresAt(expiresAt);
        if (binding.getId() == null) {
            bindingMapper.insert(binding);
        } else {
            bindingMapper.updateById(binding);
        }
    }

    private UserOauthBinding selectByProviderOpenid(String provider, String openid) {
        return bindingMapper.selectOne(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getProvider, provider)
                .eq(UserOauthBinding::getOpenid, openid));
    }

    private boolean hasNoOtherBinding(long userId, String excludeProvider) {
        return bindingMapper.selectCount(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getUserId, userId)
                .ne(UserOauthBinding::getProvider, excludeProvider)) == 0;
    }

    private boolean hasPassword(long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase();
        if (!GITHUB.equals(normalized) && !GITEE.equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的 OAuth 平台");
        }
        return normalized;
    }
}
