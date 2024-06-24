package it.polimi.blackjackbe.service.implementation;

import it.polimi.blackjackbe.builder.ManoBuilder;
import it.polimi.blackjackbe.dto.request.EndTavoloRequest;
import it.polimi.blackjackbe.dto.response.CartaResponse;
import it.polimi.blackjackbe.exception.BadRequestException;
import it.polimi.blackjackbe.exception.NotFoundException;
import it.polimi.blackjackbe.model.*;
import it.polimi.blackjackbe.repository.ManoRepository;
import it.polimi.blackjackbe.repository.TavoloRepository;
import it.polimi.blackjackbe.repository.UserRepository;
import it.polimi.blackjackbe.model.TavoloStatus;
import it.polimi.blackjackbe.dto.response.TavoloStatusResponse;
import it.polimi.blackjackbe.service.definition.TavoloService;
import it.polimi.blackjackbe.singleton.SingletonTavoli;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service per gestire tutte le operazioni relative ai tavoli.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TavoloServiceImplementation implements TavoloService {

    private final TavoloRepository tavoloRepository;
    private final UserRepository userRepository;
    private final ManoRepository manoRepository;

    /**
     * Inizializza un nuovo tavolo per l'utente specificato.
     *
     * @param tipoTavolo Tipo del tavolo da inizializzare.
     * @param userId     ID dell'utente che inizializza il tavolo.
     * @throws BadRequestException se l'ID dell'utente o il tipo del tavolo non sono validi.
     */
    @Override
    public void init(String tipoTavolo, Long userId) {
        // Verifica che l'ID dell'utente sia valido.
        if (userId == null || userId <= 0) {
            throw new BadRequestException("User id is required");
        }

        // Verifica che il tipo del tavolo sia valido.
        if (tipoTavolo == null || tipoTavolo.isEmpty()) {
            throw new BadRequestException("Table type is required");
        }

        // Controlla se l'utente esiste nel database.
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new BadRequestException("User not found");
        }

        // Crea un nuovo tavolo per l'utente e salva nel database.
        Tavolo tavolo = SingletonTavoli.getInstance().createTable(user.get(), TipoTavolo.valueOf(tipoTavolo));
        tavoloRepository.save(tavolo);

        // Inizializza le carte per il tavolo.
        tavolo.initCarte();
    }

    /**
     * Termina il tavolo per l'utente specificato.
     *
     * @param userId  ID dell'utente che termina il tavolo.
     * @param request Richiesta contenente i dati per terminare il tavolo.
     * @throws BadRequestException se l'utente o il tavolo non vengono trovati.
     */
    @Override
    public void end(Long userId, EndTavoloRequest request) {
        // Controlla se l'utente esiste nel database.
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new BadRequestException("User not found");
        }

        // Controlla se il tavolo esiste nel database.
        Optional<Tavolo> tavolo = tavoloRepository.findByPlayer(user.get());
        if (tavolo.isEmpty()) {
            throw new BadRequestException("Table not found");
        }

        // Salva lo stato del tavolo e termina la sessione.
        tavoloRepository.save(tavolo.get());
        tavolo.get().end();
    }

    /**
     * Fornisce lo stato corrente del tavolo e altre informazioni rilevanti.
     *
     * @param tavolo       Tavolo di cui ottenere lo stato.
     * @param tavoloStatus Stato attuale del tavolo.
     * @return Un oggetto TavoloStatusResponse contenente le informazioni sullo stato del tavolo.
     */
    public TavoloStatusResponse getTavoloStatusResponse(Tavolo tavolo, TavoloStatus tavoloStatus) {
        // Recupera l'utente dal repository.
        User user = userRepository.findById(tavolo.getPlayer().getUserId()).get();

        // Costruisce e restituisce la risposta con le informazioni sullo stato del tavolo.
        return new TavoloStatusResponse(
                tavolo.getCarteSingolaManoPlayer().stream().map(carta -> new CartaResponse(carta.getSeme(), carta.getValore(), carta.getPunteggio(), carta.getOrder())).toList(),
                tavolo.punteggioUtente(),
                tavolo.getCarteSingolaManoDealer().stream().map(carta -> new CartaResponse(carta.getSeme(), carta.getValore(), carta.getPunteggio(), carta.getOrder())).toList(),
                tavolo.punteggioDealer(),
                tavoloStatus,
                user.getSaldo(),
                tavolo.getTotalWinning()
        );
    }

    /**
     * Recupera il tavolo associato all'utente specificato.
     *
     * @param userId ID dell'utente di cui recuperare il tavolo.
     * @return Un oggetto Tavolo associato all'utente specificato.
     * @throws BadRequestException   se l'utente non viene trovato.
     * @throws IllegalStateException se la mano non è stata distribuita.
     */
    public Tavolo getTavolo(Long userId) {
        // Controlla se l'utente esiste nel database.
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new BadRequestException("User not found");
        }

        // Recupera il tavolo associato all'utente.
        Tavolo tavolo = SingletonTavoli.getInstance().getTable(user.get());

        // Verifica se la mano è stata distribuita correttamente.
        if (tavolo.getCarteSingolaManoPlayer().size() < 2) {
            throw new IllegalStateException("The deal was not made");
        }
        return tavolo;
    }

    /**
     * Processa la vincita del giocatore per il tavolo specificato.
     *
     * @param tavolo       Tavolo di cui processare la vincita.
     * @param tavoloStatus Stato del tavolo dopo la partita.
     */
    public void processWin(Tavolo tavolo, TavoloStatus tavoloStatus) {
        processWin(tavolo, tavoloStatus, false);
    }

    /**
     * Processa la vincita del giocatore per il tavolo specificato, con un'opzione per il blackjack.
     *
     * @param tavolo       Tavolo di cui processare la vincita.
     * @param tavoloStatus Stato del tavolo dopo la partita.
     * @param blackJack    Indica se il giocatore ha vinto con un blackjack.
     */
    public void processWin(Tavolo tavolo, TavoloStatus tavoloStatus, boolean blackJack) {
        // Verifica che il tavolo non sia nullo.
        if (tavolo == null) {
            throw new BadRequestException("Tavolo non può essere null");
        }

        // Recupera l'utente associato al tavolo e l'amministratore dal repository.
        User user = userRepository.findById(tavolo.getPlayer().getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        User admin = userRepository.findByRuolo(Ruolo.ADMIN)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        Double importo = 0.0;

        // Log iniziale dei saldi
        System.out.println("Initial User Saldo: " + user.getSaldo());
        System.out.println("Initial Admin Saldo: " + admin.getSaldo());

        // Calcola le vincite e aggiorna i saldi in base allo stato del tavolo.
        if (tavoloStatus == TavoloStatus.PLAYER_WIN) {
            // Se il giocatore ha vinto, calcola la vincita netta in base alla puntata dell'utente.
            Double rate = blackJack ? 2.5 : 2.0;
            Double vincitaNetta = tavolo.getPlotUser() * rate; // Vincita totale del giocatore
            Double totaleDaPagareDalCasino = tavolo.getPlotUser() * (rate - 1); // Totale da pagare dal casinò

            user.setSaldo(user.getSaldo() + vincitaNetta); // Aggiunge la vincita al saldo dell'utente.
            admin.setSaldo(admin.getSaldo() - totaleDaPagareDalCasino); // Sottrae solo la vincita netta dal saldo dell'amministratore.
            tavolo.setTotalWinning(tavolo.getTotalWinning() + vincitaNetta); // Aggiorna le vincite totali del tavolo.
            importo = -totaleDaPagareDalCasino / 2; // Imposta l'importo per la registrazione della mano.

            // Log della vincita del giocatore
            System.out.println("Player won. Winning Amount: " + vincitaNetta);
        } else if (tavoloStatus == TavoloStatus.PLAYER_LOSE) {
            // Se il giocatore ha perso, la puntata dell'utente è già stata sottratta nel metodo `deal`,
            // quindi dobbiamo solo aggiornare il saldo dell'amministratore.
            admin.setSaldo(admin.getSaldo() + tavolo.getPlotUser()); // Incrementa il saldo dell'admin
            tavolo.setTotalWinning(tavolo.getTotalWinning() - tavolo.getPlotUser()); // Aggiorna le vincite totali del tavolo.
            importo = tavolo.getPlotUser(); // Imposta l'importo per la registrazione della mano.

            // Log della perdita del giocatore
            System.out.println("Player lost. Betting Amount: " + tavolo.getPlotUser());
        } else if (tavoloStatus == TavoloStatus.DRAW) {
            // Se la partita è in pareggio, la puntata dell'utente viene restituita al saldo dell'utente.
            user.setSaldo(user.getSaldo() + tavolo.getPlotUser());

            // Log del pareggio
            System.out.println("It's a draw. Returned Betting Amount: " + tavolo.getPlotUser());
        }

        // Registra la mano nel database.
        Mano mano = new ManoBuilder()
                .tavolo(tavolo)
                .dataMano(LocalDateTime.now())
                .importo(importo)
                .build();
        manoRepository.save(mano);

        // Salva le modifiche ai saldi dell'utente e dell'admin nel database.
        userRepository.save(user);
        userRepository.save(admin);

        // Log finale dei saldi
        System.out.println("Final User Saldo: " + user.getSaldo());
        System.out.println("Final Admin Saldo: " + admin.getSaldo());
    }
}
