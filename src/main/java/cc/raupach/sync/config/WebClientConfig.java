package cc.raupach.sync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${sync.shopware.token-uri}")
    private String tokenUri;

    @Value("${sync.shopware.client-id}")
    private String clientId;

    @Value("${sync.shopware.client-secret}")
    private String clientSecret;

    @Bean
    ReactiveClientRegistrationRepository clientRegistrations() {

        ClientRegistration registration = ClientRegistration
                .withRegistrationId("sw")
                .tokenUri(tokenUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        return new InMemoryReactiveClientRegistrationRepository(registration);
    }

    @Bean(name = "shopware")
    WebClient shopwareWebClient(ReactiveClientRegistrationRepository clientRegistrations) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, new UnAuthenticatedServerOAuth2AuthorizedClientRepository());
        oauth.setDefaultClientRegistrationId("sw");

        return WebClient.builder()
                .filter(oauth)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                        .build())
                .build();
    }

    @Bean(name = "printful")
    WebClient printfulWebClient() {

        return WebClient.builder()
                .build();
    }
}
