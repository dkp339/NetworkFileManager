package com.rubberhuman.server.controller.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rubberhuman.server.dto.auth.LoginRequest;
import com.rubberhuman.server.dto.auth.LoginResponse;
import com.rubberhuman.server.dto.auth.RegisterRequest;
import com.rubberhuman.server.entity.auth.SysUser;
import com.rubberhuman.server.mapper.auth.SysUserMapper;
import com.rubberhuman.server.service.auth.SysUserDetailsService;
import com.rubberhuman.server.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SysUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private SysUserDetailsService sysUserService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "认证失败"));
        }

        // 生成 Token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("ROLE_USER");

        return ResponseEntity.ok(new LoginResponse(token, userDetails.getUsername(), role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        sysUserService.register(req);
        return ResponseEntity.ok(Map.of("message", "注册成功，请登录"));
    }
}