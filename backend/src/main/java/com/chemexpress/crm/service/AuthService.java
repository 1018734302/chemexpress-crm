package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.security.CurrentUser;
import com.chemexpress.crm.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppMetaService appMetaService;

    public Map<String, Object> login(Requests.Login request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim()));
        if (user == null) {
            throw new BizException(400, "账号或密码不正确");
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BizException(403, "账号已被停用，请联系管理员");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(400, "账号或密码不正确");
        }
        String token = jwtService.createToken(user, appMetaService.getDataVersion());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", toUserInfo(user));
        return result;
    }

    public Map<String, Object> me(CurrentUser current) {
        User user = userMapper.selectById(current.id());
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BizException(401, "账号不可用，请重新登录");
        }
        return toUserInfo(user);
    }

    public Map<String, Object> toUserInfo(User user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("name", user.getName());
        info.put("role", user.getRole());
        info.put("roleName", Constants.label(Constants.ROLES, user.getRole()));
        info.put("region", user.getRegion());
        return info;
    }
}
