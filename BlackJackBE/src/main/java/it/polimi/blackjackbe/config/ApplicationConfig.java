package it.polimi.blackjackbe.config;

import it.polimi.blackjackbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean utili per la configurazione generale dell'applicazione. Grazie all'annotazione "@Configuration",
 * il contenuto di questa classe viene eseguito appena il server viene avviato.
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
    private final UserRepository userRepository;

    /**
     * Bean per il servizio di gestione dei dettagli dell'utente.
     * Dato un username, viene preso l'utente dal database.
     * @return Un'istanza di UserDetailsService che carica i dettagli dell'utente.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));
    }

    /**
     * Bean che specifica quale metodo utilizzare per codificare la password.
     * @return Un'istanza dell'encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean per la configurazione del provider di autenticazione.
     * Include il servizio di gestione dei dettagli dell'utente e il codificatore di password.
     * @return Un'istanza di AuthenticationProvider configurata.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Specifica il service da utilizzare per prendere i dati dell'utente dal db.
        provider.setUserDetailsService(userDetailsService());
        // Specifica il metodo per codificare la password.
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Bean che specifica come gestire l'autenticazione.
     * @param authenticationConfiguration Configurazione dell'AuthenticationManager.
     * @return Un'istanza dell'AuthenticationManager.
     * @throws Exception Eccezione generale che può essere lanciata durante la configurazione.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}