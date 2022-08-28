package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
class SecurityConfiguration {

	private final DatabaseClient db;

	private final String sqlSelectUsers = " select * from stb_users where username = :username ";

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity http) {
		return http//
				.authorizeExchange(ae -> ae.anyExchange().authenticated()) //
				.httpBasic(Customizer.withDefaults()) //
				.csrf(ServerHttpSecurity.CsrfSpec::disable) //
				.build();
	}

	@Bean
	ReactiveUserDetailsService authentication() {
		return username -> db //
				.sql(this.sqlSelectUsers)//
				.bind("username", username)//
				.fetch()//
				.one()//
				.map(record -> new User((String) record.get("username"), (String) record.get("password"), true, true,
						true, true, List.of(new SimpleGrantedAuthority("USER"))));
	}

}
