package com.back.global.security.oauth2.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.*;
import java.util.stream.Collectors;

public class CustomTokenResponseConverter implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    private static final Set<String> TOKEN_RESPONSE_PARAMETER_NAMES = Set.of(
            OAuth2ParameterNames.ACCESS_TOKEN,
            OAuth2ParameterNames.TOKEN_TYPE,
            OAuth2ParameterNames.EXPIRES_IN,
            OAuth2ParameterNames.REFRESH_TOKEN,
            OAuth2ParameterNames.SCOPE
    );

    @Override
    public OAuth2AccessTokenResponse convert(Map<String, Object> tokenResponseParameters) {
        String accessToken = (String) tokenResponseParameters.get(OAuth2ParameterNames.ACCESS_TOKEN);
        OAuth2AccessToken.TokenType tokenType = OAuth2AccessToken.TokenType.BEARER;

        long expiresIn = 0;
        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.EXPIRES_IN)) {
            expiresIn = Long.parseLong(tokenResponseParameters.get(OAuth2ParameterNames.EXPIRES_IN).toString());
        }

        Set<String> scopes = Collections.emptySet();
        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.SCOPE)) {
            String scope = (String) tokenResponseParameters.get(OAuth2ParameterNames.SCOPE);
            scopes = Arrays.stream(scope.split(" ")).collect(Collectors.toSet());
        }

        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        tokenResponseParameters.forEach((key, value) -> {
            if (!TOKEN_RESPONSE_PARAMETER_NAMES.contains(key)) {
                additionalParameters.put(key, value);
            }
        });

        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.REFRESH_TOKEN)) {
            additionalParameters.put(
                    OAuth2ParameterNames.REFRESH_TOKEN,
                    tokenResponseParameters.get(OAuth2ParameterNames.REFRESH_TOKEN)
            );
        }

        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .scopes(scopes)
                .additionalParameters(additionalParameters);

        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.REFRESH_TOKEN)) {
            builder.refreshToken((String) tokenResponseParameters.get(OAuth2ParameterNames.REFRESH_TOKEN));
        }

        return builder.build();
    }
}