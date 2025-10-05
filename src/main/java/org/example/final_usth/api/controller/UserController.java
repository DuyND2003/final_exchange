package org.example.final_usth.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.api.model.*;
import org.example.final_usth.marketdata.entity.User;
import org.example.final_usth.marketdata.manager.UserManager;
import org.example.final_usth.marketdata.repository.UserRepository;
import org.example.final_usth.matchingengine.command.DepositCommand;
import org.example.final_usth.matchingengine.command.MatchingEngineCommandProducer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final MatchingEngineCommandProducer matchingEngineCommandProducer;

    @GetMapping("/users/self")
    public UserDto getCurrentUser(@RequestAttribute(required = false) User currentUser) {
//        logger.info("curren user" + currentUser);
//        if (currentUser == null) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
//        }
//        return userDto(currentUser);
        UserDto userDto = new UserDto();
        userDto.setId("1");
        userDto.setEmail("duynd@gmail.com");
        userDto.setName("duynd");

        return userDto;
    }

    @PutMapping("/users/self")
    public UserDto updateProfile(@RequestBody UpdateProfileRequest updateProfileRequest,
                                 @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        // Update nickname nếu có
        if (updateProfileRequest.getNickName() != null) {
            currentUser.setNickName(updateProfileRequest.getNickName());
        }
        // Update 2FA type nếu có
        if (updateProfileRequest.getTwoStepVerificationType() != null) {
            currentUser.setTwoStepVerificationType(updateProfileRequest.getTwoStepVerificationType());
        }
        // Lưu vào DB
        userRepository.save(currentUser);

        return userDto(currentUser);
    }

    @PostMapping("/users/accessToken")
    public TokenDto signIn(@RequestBody @Valid SignInRequest signInRequest, HttpServletRequest request,
                           HttpServletResponse response) {
        User user = userManager.getUser(signInRequest.getEmail(), signInRequest.getPassword());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "email or password error");
        }
        // Tạo access token
        String token = userManager.generateAccessToken(user, request.getSession().getId());
        // Gửi token về client kèm cookie
        addAccessTokenCookie(response, token);

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(token);
        tokenDto.setTwoStepVerification("none");
        return tokenDto;
    }

    // Khi user logout → xóa access token trong hệ thống.
    @DeleteMapping("/users/accessToken")
    public void signOut(@RequestAttribute(required = false) User currentUser,
                        @RequestAttribute(required = false) String accessToken) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        userManager.deleteAccessToken(accessToken);
    }

    // Tạo user mới + tặng vốn ảo để trade thử (demo exchange).
    @PostMapping("/users")
    public UserDto signUp(@RequestBody @Valid SignUpRequest signUpRequest) {
        User user = userManager.createUser(signUpRequest.getEmail(), signUpRequest.getPassword());

        //TODO: Recharge each user for demonstration
        deposit(user.getId(), "BTC", BigDecimal.valueOf(1000000000));
        deposit(user.getId(), "ETH", BigDecimal.valueOf(1000000000));
        deposit(user.getId(), "USDT", BigDecimal.valueOf(1000000000));

        return userDto(user);
    }

    private void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // // hết hạn sau 7 ngày
        cookie.setSecure(false); // // chưa bắt buộc HTTPS
        cookie.setHttpOnly(false); //  // vẫn có thể đọc bằng JS
        response.addCookie(cookie);
    }
    // Convert User → UserDto
    private UserDto userDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setBand(false);
        userDto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().toString() : null);
        userDto.setName(user.getNickName());
        userDto.setTwoStepVerificationType(user.getTwoStepVerificationType());
        return userDto;
    }
    // Nạp tiền vào tài khoản (demo)
    private void deposit(String userId, String currency, BigDecimal amount) {
        DepositCommand command = new DepositCommand();
        command.setUserId(userId);
        command.setCurrency(currency);
        command.setAmount(amount);
        command.setTransactionId(UUID.randomUUID().toString());
        // // Gửi command sang Matching Engine (nó sẽ update AccountBook)
        matchingEngineCommandProducer.send(command, null);
    }
}

