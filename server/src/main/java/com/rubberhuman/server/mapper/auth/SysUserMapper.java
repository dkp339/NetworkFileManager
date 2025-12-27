package com.rubberhuman.server.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rubberhuman.server.entity.auth.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
