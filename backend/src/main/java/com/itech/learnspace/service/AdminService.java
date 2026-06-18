package com.itech.learnspace.service;

import com.itech.learnspace.dto.UserCreateRequest;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(SysUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<SysUser> listUsers() {
        return userRepository.findAll();
    }

    public SysUser createUser(UserCreateRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new BusinessException("账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword() != null ? req.getPassword() : "123456"));
        user.setRealName(req.getRealName());
        user.setRole(req.getRole());
        user.setClassId(req.getClassId());
        user.setStatus(1);
        return userRepository.save(user);
    }

    public SysUser toggleStatus(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        return userRepository.save(user);
    }
}
