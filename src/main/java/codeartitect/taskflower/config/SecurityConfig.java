package codeartitect.taskflower.config;

import codeartitect.taskflower.security.CustomOAuth2UserService;
import codeartitect.taskflower.security.HttpCookieOAuth2AuthorizationRequestRepository;
import codeartitect.taskflower.security.OAuth2AuthenticationFailureHandler;
import codeartitect.taskflower.security.OAuth2AuthenticationSuccessHandler;
import codeartitect.taskflower.security.token.TokenFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        proxyTargetClass = true,
        jsr250Enabled = true
)
public class SecurityConfig {
    @Value("${app.url-base}")
    private String urlBase;
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final List<String> allowedMethods = Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name()
    );
    private final List<String> allowedHeaders = Arrays.asList(
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.AUTHORIZATION
    );

    private final TokenFilter tokenFilter;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Autowired
    public SecurityConfig(TokenFilter tokenFilter, HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository, CustomOAuth2UserService customOAuth2UserService, OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler, OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler) {
        this.tokenFilter = tokenFilter;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//        Basin settings
        httpSecurity
//                Not use HttpBasicAuth
                .httpBasic(HttpBasicConfigurer::disable)
//                Spring login form is not used.
                .formLogin(FormLoginConfigurer::disable)
//                Not use csrf config because this service is developed as a Rest API server.
                .csrf(CsrfConfigurer::disable)
//                Not session but token is used for this service.
                .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

//        CORS config
        httpSecurity
                .cors(corsConfigurer -> corsConfigurer
                        .configurationSource(corsConfigurationSource()));

//        OAuth2
        httpSecurity
                .oauth2Login(httpSecurityOAuth2LoginConfigurer -> httpSecurityOAuth2LoginConfigurer
                        .authorizationEndpoint(endpointConfig -> endpointConfig
                                .baseUri("/oauth2/authorize")
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
                        .redirectionEndpoint(redirectionEndpointConfig -> redirectionEndpointConfig
                                .baseUri("/oauth2/code/**"))
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler));

//        Request matchers
        httpSecurity
                .authorizeHttpRequests(request -> request
                        .requestMatchers(urlBase + "/auth/**").permitAll()
                        .anyRequest().denyAll()
                );

//        JWT Token filter
        httpSecurity
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(urlBase + "/**", configuration);

        return source;
    }

}
