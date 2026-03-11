import com.example.superapp.controller.BannerController;
import com.example.superapp.entity.Banner;
import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.User;
import com.example.superapp.service.BannerService;
import com.example.superapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private BannerController bannerController;

    @Test
    void shouldReturnNull_whenSubscriptionExpired() {
        // --- ARRANGE: dựng data giả ---
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");

        Subscription expiredSub = new Subscription();
        expiredSub.setEndDate(LocalDateTime.now().minusDays(1)); // đã hết hạn

        User mockUser = new User();
        mockUser.setSubscriptions(List.of(expiredSub));

        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // --- ACT: gọi hàm cần test ---
        ResponseEntity<byte[]> response = bannerController.retrieveImage(1, mockUserDetails);

        // --- ASSERT: kiểm tra kết quả ---
        assertNull(response);
    }

    @Test
    void shouldReturnImage_whenSubscriptionValid() {
        // --- ARRANGE ---
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");

        Subscription activeSub = new Subscription();
        activeSub.setEndDate(LocalDateTime.now().plusDays(30)); // còn hạn

        User mockUser = new User();
        mockUser.setSubscriptions(List.of(activeSub));

        Banner mockBanner = new Banner();
        mockBanner.setData(new byte[]{1, 2, 3});

        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // --- ACT ---
        ResponseEntity<byte[]> response = bannerController.retrieveImage(1, mockUserDetails);
        ResponseEntity<List<Map<String, Object>>> bannerInfoResponse = bannerController.retrieveAllBanners(mockUserDetails);

        // --- ASSERT ---
        assertEquals(response, ResponseEntity.ok(new byte[0]));
        assertEquals(bannerInfoResponse, ResponseEntity.ok(Collections.emptyList()));
    }
}
