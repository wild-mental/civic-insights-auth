package com.makersworld.civic_insights_auth;

import com.makersworld.civic_insights_auth.service.GoogleOAuth2Service;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class CivicInsightsAuthApplicationTests {

	@MockBean
	private GoogleOAuth2Service googleOAuth2Service;

	@Test
	void contextLoads() {
	}

}
