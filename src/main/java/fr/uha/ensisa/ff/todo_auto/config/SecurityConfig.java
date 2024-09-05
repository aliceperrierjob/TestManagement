package fr.uha.ensisa.ff.todo_auto.config;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@ComponentScan("fr.uha.ensisa.ff.todo_auto.config")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private AuthProvider authProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
    	String idForEncode = "bcrypt";
    	Map<String, PasswordEncoder> encoders = new TreeMap<>();
    	encoders.put(idForEncode, new BCryptPasswordEncoder());

    	return new DelegatingPasswordEncoder(idForEncode, encoders);
    }

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {

		auth.authenticationProvider(authProvider);
	}

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
          .csrf().disable()
          .authorizeRequests()
          .antMatchers("/api/**").authenticated()
          .anyRequest().permitAll()
          .and()
          .formLogin()
          .loginPage("/login")
          .loginProcessingUrl("/perform_login")
          .defaultSuccessUrl("/", true)
          .failureUrl("/login?error=Bad%20credentials")
          .and()
          .logout()
          .logoutUrl("/perform_logout")
          .deleteCookies("JSESSIONID")
          ;
        http.exceptionHandling().authenticationEntryPoint(new RestAuthenticationEntryPoint());
    }
}
