package it.polimi.blackjackbe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

   @Bean
   public CorsFilter corsFilter() {
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      CorsConfiguration config = new CorsConfiguration();

      // Abilita la richiesta di credenziali
      config.setAllowCredentials(true);
      // Consente l'accesso solo dall'origine del frontend
      config.setAllowedOrigins(Collections.singletonList("https://bjsaferfe-ea8eff579dd6.herokuapp.com"));
      // Consente tutti i metodi HTTP
      config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      // Consente tutti gli header
      config.setAllowedHeaders(Collections.singletonList("*"));

      source.registerCorsConfiguration("/**", config);
      return new CorsFilter(source);
   }

   @Bean
   public FilterRegistrationBean<CorsFilter> simpleCorsFilter() {
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      CorsConfiguration config = new CorsConfiguration();

      // Abilita la richiesta di credenziali
      config.setAllowCredentials(true);
      // Consente l'accesso solo dall'origine del frontend
      config.setAllowedOrigins(Collections.singletonList("https://bjsaferfe-ea8eff579dd6.herokuapp.com"));
      // Consente tutti i metodi HTTP
      config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      // Consente tutti gli header
      config.setAllowedHeaders(Collections.singletonList("*"));

      source.registerCorsConfiguration("/**", config);

      FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
      bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
      return bean;
   }
}

