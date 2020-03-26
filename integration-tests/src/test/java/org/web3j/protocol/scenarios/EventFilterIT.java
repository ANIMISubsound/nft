/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.protocol.scenarios;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Filter scenario integration tests. */
public class EventFilterIT extends Scenario {

    // Deployed Fibonacci contract instance in testnet
    private static final String CONTRACT_ADDRESS = "0x3c05b2564139fb55820b18b72e94b2178eaace7d";

    @Test
    public void testEventFilter() throws Exception {
        unlockAccount();

        final Function function = createFibonacciFunction();
        final String encodedFunction = FunctionEncoder.encode(function);

        final BigInteger gas = estimateGas(encodedFunction);
        final String transactionHash =
                sendTransaction(ALICE, CONTRACT_ADDRESS, gas, encodedFunction);

        final TransactionReceipt transactionReceipt = waitForTransactionReceipt(transactionHash);

        assertFalse(gas.equals(transactionReceipt.getGasUsed()));

        final List<Log> logs = transactionReceipt.getLogs();
        assertFalse(logs.isEmpty());

        final Log log = logs.get(0);

        final List<String> topics = log.getTopics();
        assertEquals(topics.size(), (1));

        final Event event =
                new Event(
                        "Notify",
                        Arrays.asList(
                                new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));

        // check function signature - we only have a single topic our event signature,
        // there are no indexed parameters in this example
        final String encodedEventSignature = EventEncoder.encode(event);
        assertEquals(topics.get(0), (encodedEventSignature));

        // verify our two event parameters
        final List<Type<?>> results =
                FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());
        assertEquals(
                results,
                (Arrays.asList(
                        new Uint256(BigInteger.valueOf(7)), new Uint256(BigInteger.valueOf(13)))));

        // finally check it shows up in the event filter
        final List<EthLog.LogResult> filterLogs =
                createFilterForEvent(encodedEventSignature, CONTRACT_ADDRESS);
        assertFalse(filterLogs.isEmpty());
    }

    private BigInteger estimateGas(final String encodedFunction) throws Exception {
        final EthEstimateGas ethEstimateGas =
                web3j.ethEstimateGas(
                                Transaction.createEthCallTransaction(
                                        ALICE.getAddress(), null, encodedFunction))
                        .sendAsync()
                        .get();
        // this was coming back as 50,000,000 which is > the block gas limit of 4,712,388
        // see eth.getBlock("latest")
        return ethEstimateGas.getAmountUsed().divide(BigInteger.valueOf(100));
    }

    private String sendTransaction(
            final Credentials credentials,
            final String contractAddress,
            final BigInteger gas,
            final String encodedFunction)
            throws Exception {
        final BigInteger nonce = getNonce(credentials.getAddress());
        final Transaction transaction =
                Transaction.createFunctionCallTransaction(
                        credentials.getAddress(),
                        nonce,
                        Transaction.DEFAULT_GAS,
                        gas,
                        contractAddress,
                        encodedFunction);

        final org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse =
                web3j.ethSendTransaction(transaction).sendAsync().get();

        assertFalse(transactionResponse.hasError());

        return transactionResponse.getTransactionHash();
    }

    private List<EthLog.LogResult> createFilterForEvent(
            final String encodedEventSignature, final String contractAddress) throws Exception {
        final EthFilter ethFilter =
                new EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress);

        ethFilter.addSingleTopic(encodedEventSignature);

        final EthLog ethLog = web3j.ethGetLogs(ethFilter).send();
        return ethLog.getLogs();
    }
}
