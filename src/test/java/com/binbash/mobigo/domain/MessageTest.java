package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.MessageTestSamples.*;
import static com.binbash.mobigo.domain.PeopleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Message.class);
        Message message1 = getMessageSample1();
        Message message2 = new Message();
        assertThat(message1).isNotEqualTo(message2);

        message2.setId(message1.getId());
        assertThat(message1).isEqualTo(message2);

        message2 = getMessageSample2();
        assertThat(message1).isNotEqualTo(message2);
    }

    @Test
    void expediteurTest() {
        Message message = getMessageRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        message.addExpediteur(peopleBack);
        assertThat(message.getExpediteurs()).containsOnly(peopleBack);
        assertThat(peopleBack.getMessagesExpediteur()).isEqualTo(message);

        message.removeExpediteur(peopleBack);
        assertThat(message.getExpediteurs()).doesNotContain(peopleBack);
        assertThat(peopleBack.getMessagesExpediteur()).isNull();

        message.expediteurs(new HashSet<>(Set.of(peopleBack)));
        assertThat(message.getExpediteurs()).containsOnly(peopleBack);
        assertThat(peopleBack.getMessagesExpediteur()).isEqualTo(message);

        message.setExpediteurs(new HashSet<>());
        assertThat(message.getExpediteurs()).doesNotContain(peopleBack);
        assertThat(peopleBack.getMessagesExpediteur()).isNull();
    }

    @Test
    void destinataireTest() {
        Message message = getMessageRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        message.addDestinataire(peopleBack);
        assertThat(message.getDestinataires()).containsOnly(peopleBack);
        assertThat(peopleBack.getMessagesDestinatire()).isEqualTo(message);

        message.removeDestinataire(peopleBack);
        assertThat(message.getDestinataires()).doesNotContain(peopleBack);
        assertThat(peopleBack.getMessagesDestinatire()).isNull();

        message.destinataires(new HashSet<>(Set.of(peopleBack)));
        assertThat(message.getDestinataires()).containsOnly(peopleBack);
        assertThat(peopleBack.getMessagesDestinatire()).isEqualTo(message);

        message.setDestinataires(new HashSet<>());
        assertThat(message.getDestinataires()).doesNotContain(peopleBack);
        assertThat(peopleBack.getMessagesDestinatire()).isNull();
    }
}
