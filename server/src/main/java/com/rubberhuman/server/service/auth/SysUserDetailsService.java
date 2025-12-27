package com.rubberhuman.server.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rubberhuman.server.dto.auth.RegisterRequest;
import com.rubberhuman.server.entity.auth.SysUser;
import com.rubberhuman.server.exception.BusinessException;
import com.rubberhuman.server.mapper.auth.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class SysUserDetailsService implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) {

        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(sysUser.getRole());

        return new User(sysUser.getUsername(), sysUser.getPassword(), Collections.singletonList(authority));
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (getByUsername(req.getUsername()) != null) {
            throw new BusinessException("用户名 '" + req.getUsername() + "' 已被注册");
        }

        SysUser newUser = new SysUser();
        newUser.setUsername(req.getUsername());
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));
        newUser.setRole("ROLE_USER");
        newUser.setEmail(req.getEmail());
        newUser.setCreateTime(LocalDateTime.now());

        sysUserMapper.insert(newUser);
    }

    // 辅助方法
    public Long getUserIdByUsername(String username) {
        SysUser user = getByUsername(username);
        if (user == null) {
            throw new BusinessException("用户数据异常：找不到当前登录用户");
        }
        return user.getId();
    }

    private SysUser getByUsername(String username) {
        return sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
    }
}
