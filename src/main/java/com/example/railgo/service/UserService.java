package com.example.railgo.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.dto.UpdateProfileRequest;
import com.example.railgo.data.po.User;
import com.example.railgo.data.vo.UserProfileResponse;
import com.example.railgo.mapper.AuthRefreshTokenMapper;
import com.example.railgo.mapper.UserMapper;
import com.example.railgo.data.dto.ChangePasswordRequest;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.railgo.data.dto.CreatePassengerRequest;
import com.example.railgo.data.dto.UpdatePassengerRequest;
import com.example.railgo.data.enums.IdType;
import com.example.railgo.data.enums.PassengerType;
import com.example.railgo.data.po.Passenger;
import com.example.railgo.data.vo.PassengerResponse;
import com.example.railgo.mapper.PassengerMapper;
import com.example.railgo.utils.PassengerIdentityUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    private final AuthRefreshTokenMapper
            refreshTokenMapper;

    private final PasswordEncoder passwordEncoder;

    private final VerificationCodeService
            verificationCodeService;

    private static final long MAX_PASSENGERS_PER_USER = 20;

    private final PassengerMapper passengerMapper;

    private final PassengerIdentityUtil passengerIdentityUtil;

    public UserProfileResponse getProfile(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(
            Long userId,
            UpdateProfileRequest request) {

        User user = requireUser(userId);

        if (request.phone() != null
                && !request.phone().equals(
                user.getPhone()
        )) {

            verificationCodeService.verify(
                    request.phone(),
                    request.verificationCode()
            );

            Long count = userMapper.selectCount(
                    Wrappers.<User>lambdaQuery()
                            .eq(
                                    User::getPhone,
                                    request.phone()
                            )
                            .ne(
                                    User::getId,
                                    userId
                            )
            );

            if (count > 0) {
                throw new BusinessException(
                        ErrorCode.PHONE_EXISTS
                );
            }

            user.setPhone(request.phone());
        }

        if (request.nickname() != null) {
            user.setNickname(
                    request.nickname().trim()
            );
        }

        if (request.email() != null) {
            user.setEmail(
                    request.email().isBlank()
                            ? null
                            : request.email().trim()
            );
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return toProfile(user);
    }

    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request) {

        User user = requireUser(userId);

        if (!passwordEncoder.matches(
                request.oldPassword(),
                user.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.OLD_PASSWORD_ERROR
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "新密码不能与原密码相同"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        refreshTokenMapper.revokeAllByUserId(
                userId,
                LocalDateTime.now()
        );
    }

    public UserProfileResponse toProfile(
            User user) {

        return new UserProfileResponse(
                user.getId(),
                user.getPhone(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus(),
                userMapper.selectRoleCodesByUserId(
                        user.getId()
                ),
                user.getCreatedAt()
        );
    }

    private User requireUser(Long userId) {

        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "用户不存在"
            );
        }

        return user;
    }

    @Transactional(readOnly = true)
    public List<PassengerResponse> getPassengers(
            Long userId) {

        requireUser(userId);

        return passengerMapper.selectList(
                        Wrappers
                                .<Passenger>lambdaQuery()
                                .eq(
                                        Passenger::getUserId,
                                        userId
                                )
                                .orderByDesc(
                                        Passenger::getUpdatedAt
                                )
                                .orderByDesc(
                                        Passenger::getId
                                )
                )
                .stream()
                .map(this::toPassengerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PassengerResponse getPassenger(
            Long userId,
            Long passengerId) {

        return toPassengerResponse(
                requireOwnedPassenger(
                        userId,
                        passengerId
                )
        );
    }

    @Transactional
    public PassengerResponse createPassenger(
            Long userId,
            CreatePassengerRequest request) {

        requireUser(userId);

        Long count =
                passengerMapper.selectCount(
                        Wrappers
                                .<Passenger>lambdaQuery()
                                .eq(
                                        Passenger::getUserId,
                                        userId
                                )
                );

        if (count >= MAX_PASSENGERS_PER_USER) {
            throw new BusinessException(
                    ErrorCode.PASSENGER_LIMIT_EXCEEDED
            );
        }

        String normalizedIdNo =
                validateIdentity(
                        request.idType(),
                        request.idNo()
                );

        String idNoHash =
                passengerIdentityUtil.hash(
                        normalizedIdNo
                );

        ensurePassengerIdentityUnique(
                userId,
                idNoHash,
                null
        );

        LocalDateTime now =
                LocalDateTime.now();

        Passenger passenger =
                new Passenger();

        passenger.setUserId(userId);
        passenger.setName(
                normalizePassengerName(
                        request.name()
                )
        );
        passenger.setIdType(
                request.idType().name()
        );
        passenger.setIdNoCipher(
                passengerIdentityUtil.encrypt(
                        normalizedIdNo
                )
        );
        passenger.setIdNoHash(idNoHash);
        passenger.setPassengerType(
                request.passengerType().name()
        );
        passenger.setPhone(
                normalizePassengerPhone(
                        request.phone()
                )
        );
        passenger.setCreatedAt(now);
        passenger.setUpdatedAt(now);

        try {
            passengerMapper.insert(passenger);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    ErrorCode.PASSENGER_EXISTS
            );
        }

        return toPassengerResponse(passenger);
    }

    @Transactional
    public PassengerResponse updatePassenger(
            Long userId,
            Long passengerId,
            UpdatePassengerRequest request) {

        Passenger passenger =
                requireOwnedPassenger(
                        userId,
                        passengerId
                );

        if (!request.hasChanges()) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "至少需要提交一个待修改字段"
            );
        }

        if (request.name() != null) {
            passenger.setName(
                    normalizePassengerName(
                            request.name()
                    )
            );
        }

        if (request.passengerType() != null) {
            passenger.setPassengerType(
                    request.passengerType().name()
            );
        }

        if (request.phone() != null) {
            passenger.setPhone(
                    normalizePassengerPhone(
                            request.phone()
                    )
            );
        }

        if (request.idType() != null
                || request.idNo() != null) {

            IdType idType =
                    request.idType() == null
                            ? IdType.valueOf(
                            passenger.getIdType()
                    )
                            : request.idType();

            String idNo =
                    request.idNo() == null
                            ? passengerIdentityUtil.decrypt(
                            passenger.getIdNoCipher()
                    )
                            : request.idNo();

            String normalizedIdNo =
                    validateIdentity(
                            idType,
                            idNo
                    );

            String idNoHash =
                    passengerIdentityUtil.hash(
                            normalizedIdNo
                    );

            ensurePassengerIdentityUnique(
                    userId,
                    idNoHash,
                    passengerId
            );

            passenger.setIdType(
                    idType.name()
            );
            passenger.setIdNoCipher(
                    passengerIdentityUtil.encrypt(
                            normalizedIdNo
                    )
            );
            passenger.setIdNoHash(idNoHash);
        }

        passenger.setUpdatedAt(
                LocalDateTime.now()
        );

        try {
            passengerMapper.updateById(passenger);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    ErrorCode.PASSENGER_EXISTS
            );
        }

        return toPassengerResponse(passenger);
    }

    @Transactional
    public void deletePassenger(
            Long userId,
            Long passengerId) {

        requireOwnedPassenger(
                userId,
                passengerId
        );

        try {
            passengerMapper.delete(
                    Wrappers
                            .<Passenger>lambdaQuery()
                            .eq(
                                    Passenger::getId,
                                    passengerId
                            )
                            .eq(
                                    Passenger::getUserId,
                                    userId
                            )
            );
        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new BusinessException(
                    ErrorCode.PASSENGER_IN_USE
            );
        }
    }

    private Passenger requireOwnedPassenger(
            Long userId,
            Long passengerId) {

        Passenger passenger =
                passengerMapper.selectOne(
                        Wrappers
                                .<Passenger>lambdaQuery()
                                .eq(
                                        Passenger::getId,
                                        passengerId
                                )
                                .eq(
                                        Passenger::getUserId,
                                        userId
                                )
                );

        if (passenger == null) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "乘车人不存在"
            );
        }

        return passenger;
    }

    private void ensurePassengerIdentityUnique(
            Long userId,
            String idNoHash,
            Long excludedPassengerId) {

        var query =
                Wrappers
                        .<Passenger>lambdaQuery()
                        .eq(
                                Passenger::getUserId,
                                userId
                        )
                        .eq(
                                Passenger::getIdNoHash,
                                idNoHash
                        );

        if (excludedPassengerId != null) {
            query.ne(
                    Passenger::getId,
                    excludedPassengerId
            );
        }

        if (passengerMapper.selectCount(query) > 0) {
            throw new BusinessException(
                    ErrorCode.PASSENGER_EXISTS
            );
        }
    }

    private String validateIdentity(
            IdType idType,
            String idNo) {

        String normalized =
                passengerIdentityUtil.normalize(idNo);

        if (normalized.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "证件号码不能为空"
            );
        }

        if (idType == IdType.ID_CARD
                && !isValidChineseIdCard(normalized)) {

            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "居民身份证号码格式或校验位不正确"
            );
        }

        return normalized;
    }

    private boolean isValidChineseIdCard(
            String value) {

        if (!value.matches(
                "^\\d{17}[\\dX]$"
        )) {
            return false;
        }

        int[] weights = {
                7, 9, 10, 5, 8, 4, 2,
                1, 6, 3, 7, 9, 10, 5,
                8, 4, 2
        };

        char[] checks = {
                '1', '0', 'X', '9', '8',
                '7', '6', '5', '4', '3',
                '2'
        };

        int sum = 0;

        for (int i = 0;
             i < weights.length;
             i++) {

            sum += (
                    value.charAt(i) - '0'
            ) * weights[i];
        }

        return value.charAt(17)
                == checks[sum % 11];
    }

    private String normalizePassengerName(
            String name) {

        String normalized =
                name == null
                        ? ""
                        : name.trim();

        if (normalized.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "乘车人姓名不能为空"
            );
        }

        return normalized;
    }

    private String normalizePassengerPhone(
            String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    private PassengerResponse toPassengerResponse(
            Passenger passenger) {

        String idNo =
                passengerIdentityUtil.decrypt(
                        passenger.getIdNoCipher()
                );

        return new PassengerResponse(
                passenger.getId(),
                passenger.getName(),
                IdType.valueOf(
                        passenger.getIdType()
                ),
                passengerIdentityUtil.mask(idNo),
                PassengerType.valueOf(
                        passenger.getPassengerType()
                ),
                passenger.getPhone(),
                passenger.getCreatedAt(),
                passenger.getUpdatedAt()
        );
    }
}