package SK_3team.example.planner.service;

import SK_3team.example.planner.dto.UserDTO;
import SK_3team.example.planner.dto.mapper.UserMapper;
import SK_3team.example.planner.entity.UserEntity;
import SK_3team.example.planner.repository.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    public final UserRepository userRepository;

    public void save(UserDTO userDTO){
        UserEntity userEntity = UserMapper.toUserEntity(userDTO);

        userRepository.save(userEntity);
    }

    public UserDTO login(UserDTO userDTO){
        Optional<UserEntity> byUserEmail = userRepository.findByEmail(userDTO.getEmail());
        if (byUserEmail.isPresent()){
            UserEntity userEntity = byUserEmail.get();
            if (userEntity.getPassword().equals(userDTO.getPassword())){
                return UserMapper.toUserDTO(userEntity);
            }
        }
        return null;
    }

    // 이메일 중복 확인
    public String emailCheck(String userEmail){
        Optional<UserEntity> byUserEmail = userRepository.findByEmail(userEmail);
        if (byUserEmail.isPresent()){
            return null;
        }else{
            return "ok";
        }
    }
}
