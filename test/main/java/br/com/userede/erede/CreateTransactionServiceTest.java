package br.com.userede.erede;

import br.com.userede.erede.service.CreateTransactionService;
import br.com.userede.erede.service.error.RedeException;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@RunWith(MockitoJUnitRunner.class)
public class CreateTransactionServiceTest {

	private static final String LOJA_TOKEN = "token_test";
	private static final String LOJA_FILIACAO_ID = "filiation_test";
	private static final Double TRANSACAO_VALOR_20_00 = 20.00;
	private static final String TRANSACAO_ID = "reference_test";
	private static final String CARTAO_NOME_FULANO_DE_TAL = "Fulano de Tal";
	private static final String CARTAO_ANO_EXPIRACAO_2020 = "2020";
	private static final String CARTAO_MES_EXPIRACAO_12 = "12";
	private static final String CARTAO_CODIGO_SEGURANCA_123 = "123";
	private static final String CARTAO_NUMERO_5448280000000007 = "5448280000000007";
	private static final String ENDPOINT_MOCK = "http://localhost:1080/credit";
	public static final String MOCK_URL_SERVER_LOCALHOST = "localhost";
	public static final int MOCK_URL_SERVER_PORT_1080 = 1080;
	public static final String MOCK_URL_SERVER_METHOD_POST = "POST";
	public static final String MOCK_URL_SERVER_PATH_CREDIT = "/credit";
	public static final int MOCK_URL_SERVER_STATUS_CODE_200 = 200;
	public static final int MOCK_URL_SERVER_STATUS_CODE_400 = 400;

	private ClientAndServer mockServer;

	@Mock
	private Environment environment;

	@Mock
	private Store store;

	@Mock
	private Logger logger;

	@Before
	public void startServer() {
		mockServer = startClientAndServer(MOCK_URL_SERVER_PORT_1080);
	}

	@Test
	public void aoCriarTransacaoDadoQueSejaInformadoDadosValidosQuandoAhTransacaoForAceitaDeveriaRetornaSucesso(){

		new MockServerClient(MOCK_URL_SERVER_LOCALHOST, MOCK_URL_SERVER_PORT_1080)
				.when(request()
						.withMethod(MOCK_URL_SERVER_METHOD_POST)
						.withPath(MOCK_URL_SERVER_PATH_CREDIT))
				.respond(response()
						.withStatusCode(MOCK_URL_SERVER_STATUS_CODE_200));

		when(environment.getEndpoint(any(String.class))).thenReturn(ENDPOINT_MOCK);

		when(store.getEnvironment()).thenReturn(environment);
		when(store.getFiliation()).thenReturn(LOJA_FILIACAO_ID);
		when(store.getToken()).thenReturn(LOJA_TOKEN);

		Transaction transaction = new Transaction(TRANSACAO_VALOR_20_00, TRANSACAO_ID)
				.creditCard(CARTAO_NUMERO_5448280000000007, CARTAO_CODIGO_SEGURANCA_123,
						CARTAO_MES_EXPIRACAO_12, CARTAO_ANO_EXPIRACAO_2020,
						CARTAO_NOME_FULANO_DE_TAL);

		CreateTransactionService createTransactionService = new CreateTransactionService(store, transaction, logger);
		createTransactionService.execute();

		HttpRequest[] recordedRequests = new MockServerClient(MOCK_URL_SERVER_LOCALHOST, MOCK_URL_SERVER_PORT_1080)
				.retrieveRecordedRequests(request()
						.withMethod(MOCK_URL_SERVER_METHOD_POST)
						.withPath(MOCK_URL_SERVER_PATH_CREDIT));

		assertEquals("{\n" +
				"  \"method\" : \"POST\",\n" +
				"  \"path\" : \"/credit\",\n" +
				"  \"headers\" : [ {\n" +
				"    \"name\" : \"User-Agent\",\n" +
				"    \"values\" : [ \"eRede/1.1.0 (Java; filiation_test)\" ]\n" +
				"  }, {\n" +
				"    \"name\" : \"Accept\",\n" +
				"    \"values\" : [ \"application/json\" ]\n" +
				"  }, {\n" +
				"    \"name\" : \"Content-Type\",\n" +
				"    \"values\" : [ \"application/json\" ]\n" +
				"  }, {\n" +
				"    \"name\" : \"Authorization\",\n" +
				"    \"values\" : [ \"Basic ZmlsaWF0aW9uX3Rlc3Q6dG9rZW5fdGVzdA==\" ]\n" +
				"  }, {\n" +
				"    \"name\" : \"Content-Length\",\n" +
				"    \"values\" : [ \"193\" ]\n" +
				"  }, {\n" +
				"    \"name\" : \"Host\",\n" +
				"    \"values\" : [ \"localhost:1080\" ]\n" +
				"  }, {\n" +
				"    \"name\" : \"Accept-Encoding\",\n" +
				"    \"values\" : [ \"gzip,deflate\" ]\n" +
				"  } ],\n" +
				"  \"keepAlive\" : true,\n" +
				"  \"secure\" : false,\n" +
				"  \"body\" : {\n" +
				"    \"contentType\" : \"text/plain; charset=utf-8\",\n" +
				"    \"type\" : \"STRING\",\n" +
				"    \"string\" : \"{\\\"amount\\\":2000,\\\"cardHolderName\\\":\\\"Fulano de Tal\\\",\\\"cardNumber\\\":\\\"5448280000000007\\\",\\\"expirationMonth\\\":\\\"12\\\",\\\"expirationYear\\\":\\\"2020\\\",\\\"kind\\\":\\\"credit\\\",\\\"reference\\\":\\\"reference_test\\\",\\\"securityCode\\\":\\\"123\\\"}\"\n" +
				"  }\n" +
				"}",recordedRequests[0].toString());
	}

	@Test(expected = RedeException.class)
	public void aoCriarTransacaoDadoQueSejaInformadoDadosValidosQuandoAhTransacaoForNegadaDeveriaRetornaExcecao() {

		TransactionResponse transactionResponse = new TransactionResponse();
		transactionResponse.setReturnMessage("excecao");
		transactionResponse.setReturnCode("25");

		new MockServerClient(MOCK_URL_SERVER_LOCALHOST, MOCK_URL_SERVER_PORT_1080)
				.when(request()
						.withMethod(MOCK_URL_SERVER_METHOD_POST)
						.withPath(MOCK_URL_SERVER_PATH_CREDIT))
				.respond(response()
						.withBody(new Gson().toJson(transactionResponse))
						.withStatusCode(MOCK_URL_SERVER_STATUS_CODE_400));

		when(environment.getEndpoint(any(String.class))).thenReturn(ENDPOINT_MOCK);

		when(store.getEnvironment()).thenReturn(environment);
		when(store.getFiliation()).thenReturn(LOJA_FILIACAO_ID);
		when(store.getToken()).thenReturn(LOJA_TOKEN);

		Transaction transaction = new Transaction(TRANSACAO_VALOR_20_00, TRANSACAO_ID)
				.creditCard(CARTAO_NUMERO_5448280000000007, CARTAO_CODIGO_SEGURANCA_123,
						CARTAO_MES_EXPIRACAO_12, CARTAO_ANO_EXPIRACAO_2020,
						CARTAO_NOME_FULANO_DE_TAL);

		CreateTransactionService createTransactionService = new CreateTransactionService(store, transaction, logger);
		createTransactionService.execute();
	}

	@After
	public void stopServer() {
		mockServer.stop();
	}

}
