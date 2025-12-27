package com.rubberhuman.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rubberhuman.server.entity.SysUser;
import com.rubberhuman.server.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SysUserDetailsService implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

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
}
