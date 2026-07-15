package org.example.boilerserver.mapper;

import org.example.boilerpojo.AdminUserQueryDTO;
import org.example.boilerpojo.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    UserEntity getByUserId(String userId);

    UserEntity getByUsername(String username);

    int countByUsername(String username);

    int insert(UserEntity userEntity);

    int update(UserEntity userEntity);

    List<UserEntity> listAllUsers();

    List<UserEntity> listUsers(AdminUserQueryDTO queryDTO);

    int countAdministratorByUserId(String userId);
}
