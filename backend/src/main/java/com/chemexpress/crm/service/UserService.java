package com.chemexpress.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chemexpress.crm.common.BizException;
import com.chemexpress.crm.common.Constants;
import com.chemexpress.crm.common.PageResult;
import com.chemexpress.crm.dto.Requests;
import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.security.CurrentUser;
import com.chemexpress.crm.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(String keyword, String role, String region, Boolean enabled,
                                   long pageNo, long pageSize, CurrentUser current) {
        requireAdmin(current);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            wrapper.and(query -> query
                    .like(User::getUsername, key)
                    .or()
                    .like(User::getName, key)
                    .or()
                    .like(User::getPhone, key));
        }
        wrapper.eq(StringUtils.hasText(role), User::getRole, role);
        wrapper.eq(StringUtils.hasText(region), User::getRegion, region);
        wrapper.eq(enabled != null, User::getEnabled, enabled);
        wrapper.orderByAsc(User::getId);

        Page<User> page = new Page<>(Math.max(1, pageNo), Math.min(100, Math.max(1, pageSize)));
        userMapper.selectPage(page, wrapper);
        List<UserVO> rows = page.getRecords().stream().map(UserVO::from).toList();
        return new PageResult<>(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public UserVO create(Requests.UserSave request, CurrentUser current) {
        requireAdmin(current);
        String username = trim(request.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new BizException("请输入账号");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BizException("账号需为 3-30 位字母、数字、点、下划线或短横线");
        }
        ensureUsernameUnique(username, null);
        validateRoleAndRegion(request.getRole(), request.getRegion());
        if (request.getPassword() == null || request.getPassword().length() < 6
                || request.getPassword().length() > 32) {
            throw new BizException("密码需为 6-32 个字符");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setRole(request.getRole());
        user.setRegion(request.getRegion());
        user.setPhone(trim(request.getPhone()));
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return UserVO.from(user);
    }

    @Transactional
    public UserVO update(Long id, Requests.UserSave request, CurrentUser current) {
        requireAdmin(current);
        User user = load(id);
        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().trim().equals(user.getUsername())) {
            throw new BizException("账号创建后不可修改");
        }
        validateRoleAndRegion(request.getRole(), request.getRegion());

        boolean self = user.getId().equals(current.id());
        if (self && !request.getRole().equals(user.getRole())) {
            throw new BizException("不能修改自己的角色");
        }
        boolean nextEnabled = request.getEnabled() == null
                ? !Boolean.FALSE.equals(user.getEnabled())
                : request.getEnabled();
        if (self && !nextEnabled) {
            throw new BizException("不能停用当前登录账号");
        }
        boolean losesAdmin = Constants.ROLE_ADMIN.equals(user.getRole())
                && (!Constants.ROLE_ADMIN.equals(request.getRole()) || !nextEnabled);
        if (losesAdmin) {
            ensureAnotherAdmin(user.getId());
        }

        user.setName(request.getName().trim());
        user.setRole(request.getRole());
        user.setRegion(request.getRegion());
        user.setPhone(trim(request.getPhone()));
        user.setEnabled(nextEnabled);
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    @Transactional
    public void resetPassword(Long id, Requests.PasswordReset request, CurrentUser current) {
        requireAdmin(current);
        User user = load(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    private void requireAdmin(CurrentUser current) {
        if (!current.isAdmin()) {
            throw BizException.forbidden("只有系统管理员可以维护账号");
        }
    }

    private User load(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "账号不存在");
        }
        return user;
    }

    private void validateRoleAndRegion(String role, String region) {
        if (!Constants.ROLES.containsKey(role)) {
            throw new BizException("角色不正确");
        }
        if (!Constants.REGIONS.containsKey(region)) {
            throw new BizException("区域不正确");
        }
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(excludeId != null, User::getId, excludeId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(409, "账号已存在，请更换");
        }
    }

    private void ensureAnotherAdmin(Long excludeId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, Constants.ROLE_ADMIN)
                .eq(User::getEnabled, true)
                .ne(User::getId, excludeId));
        if (count == 0) {
            throw new BizException("系统至少需要保留一个启用的管理员");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
