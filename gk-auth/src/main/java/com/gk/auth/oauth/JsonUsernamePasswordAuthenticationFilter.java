package com.gk.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.tools.DataMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.InputStream;

public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (request.getContentType() != null && request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            try (InputStream is = request.getInputStream()) {
                DataMap dataMap = objectMapper.readValue(is, DataMap.class);
                String username = dataMap.getStr("username");
                String password = dataMap.getStr("password");
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
                setDetails(request, token);
                return this.getAuthenticationManager().authenticate(token);
            } catch (IOException e) {
                throw new AuthenticationServiceException("Invalid JSON login request");
            }
        }
        return super.attemptAuthentication(request, response);
    }
}
