package SK_3team.example.planner.dto.mapper;

import SK_3team.example.planner.dto.UserDTO;
import SK_3team.example.planner.entity.UserEntity;

public class UserMapper {
    public static UserDTO toUserDTO(UserEntity userEntity){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userEntity.getId());
        userDTO.setEmail(userEntity.getEmail());
        userDTO.setUsername(userEntity.getUsername());
        userDTO.setPassword(userEntity.getPassword());
        userDTO.setCreatedAt(userEntity.getCreatedAt());
        userDTO.setRole(userEntity.getRole());
        return userDTO;
    }

//    public static UserEntity toUserEntity(UserDTO userDTO){
//        UserEntity userEntity = new UserEntity();
//        userEntity.setId(userDTO.getId());
//        userEntity.setEmail(userDTO.getEmail());
//        userEntity.setUsername(userDTO.getUsername());
//        userEntity.setPassword(userDTO.getPassword());
//        userEntity.setCreatedAt(userDTO.getCreatedAt());
//        userEntity.setRule(userDTO.getRule());
//        return userEntity;
//    }
}
