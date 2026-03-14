package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.domain.enumeration.PaymentStatusEnum;
import com.binbash.mobigo.repository.PaymentRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentScheduler.class);

    private final PaymentRepository paymentRepository;
    private final NotificationEventService notificationEventService;

    public PaymentScheduler(PaymentRepository paymentRepository, NotificationEventService notificationEventService) {
        this.paymentRepository = paymentRepository;
        this.notificationEventService = notificationEventService;
    }

    /**
     * Every 2 minutes, check for EN_COURS payments older than today and mark as ECHOUE.
     */
    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void expireStalePayments() {
        List<Payment> stalePayments = paymentRepository.findByStatut(PaymentStatusEnum.EN_COURS);
        for (Payment payment : stalePayments) {
            if (payment.getDatePaiement() != null && payment.getDatePaiement().isBefore(LocalDate.now())) {
                payment.setStatut(PaymentStatusEnum.ECHOUE);
                paymentRepository.save(payment);
                if (payment.getBooking() != null) {
                    notificationEventService.onPaymentFailed(payment.getBooking());
                }
                LOG.info(
                    "Expired stale payment {} for booking {}",
                    payment.getId(),
                    payment.getBooking() != null ? payment.getBooking().getId() : "unknown"
                );
            }
        }
    }
}
