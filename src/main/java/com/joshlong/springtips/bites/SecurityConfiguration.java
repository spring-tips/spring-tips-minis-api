package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

@Configuration
@RequiredArgsConstructor
class SecurityConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity http) {
		return http//
				.authorizeExchange(ae -> ae.matchers(EndpointRequest.toAnyEndpoint()).permitAll())
				.authorizeExchange(ae -> ae.anyExchange().authenticated()) //
				.httpBasic(Customizer.withDefaults()) //
				.csrf(ServerHttpSecurity.CsrfSpec::disable) //
				.build();
	}

	@Bean
	ReactiveUserDetailsService authentication(DatabaseClient db) {
		var sqlSelectUsers = " select * from stb_users where username = :username ";
		return username -> db //
				.sql(sqlSelectUsers)//
				.bind("username", username)//
				.fetch()//
				.one()//
				.map(record -> new User((String) record.get("username"), (String) record.get("password"), true, true,
						true, true, List.of(new SimpleGrantedAuthority("USER"))));
	}

}
