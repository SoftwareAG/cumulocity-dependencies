package org.jcouchdb.document;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.svenson.JSONParser;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
public class ParsingAcceptedDocumentTest {

    static final Logger LOG = mock(Logger.class);

    String httpAcceptedResponse = "{\"accepted\":\"ok\"}";

    @BeforeClass
    public static void prepareStaticMocks() {
        mockStatic(LoggerFactory.class, Mockito.CALLS_REAL_METHODS);
        
        when(LoggerFactory.getLogger(DocumentInfo.class)).thenReturn(LOG);
    }
    
    @Before
    public void setup() {
        reset(LOG);
    }

    @Test
    public void shouldParse() throws Exception {
        DocumentInfo document = JSONParser.defaultJSONParser().parse(DocumentInfo.class, httpAcceptedResponse);

        assertThat(document, is(notNullValue()));
    }

    @Test
    public void shouldLogErrorWhenDocumentWasAccepted() throws Exception {
        JSONParser.defaultJSONParser().parse(DocumentInfo.class, httpAcceptedResponse);

        verify(LOG, times(1)).warn(anyString());
    }
}
