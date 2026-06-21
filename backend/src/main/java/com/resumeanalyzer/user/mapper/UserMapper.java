package com.resumeanalyzer.user.mapper;

import com.resumeanalyzer.user.domain.User;
import com.resumeanalyzer.user.dto.UserDto;
import org.mapstruct.Mapper;

/** Maps {@link User} entities to API-safe DTOs. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);
}
