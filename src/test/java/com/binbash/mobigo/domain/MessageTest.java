package com.binbash.mobigo.domain;

import static com.binbash.mobigo.domain.MessageTestSamples.*;
import static com.binbash.mobigo.domain.PeopleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.web.rest.TestUtil;
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

        message.setExpediteur(peopleBack);
        assertThat(message.getExpediteur()).isEqualTo(peopleBack);

        message.expediteur(null);
        assertThat(message.getExpediteur()).isNull();
    }

    @Test
    void destinataireTest() {
        Message message = getMessageRandomSampleGenerator();
        People peopleBack = getPeopleRandomSampleGenerator();

        message.setDestinataire(peopleBack);
        assertThat(message.getDestinataire()).isEqualTo(peopleBack);

        message.destinataire(null);
        assertThat(message.getDestinataire()).isNull();
    }
}
