package org.web3j.generated;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteTransaction;
import org.web3j.protocol.core.generated.RemoteFunctionCall1;
import org.web3j.protocol.core.generated.RemoteTransaction0;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.0.1.
 */
public class SimpleStorage extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50600560005560bf806100246000396000f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166360fe47b18114604d5780636d4ce63c146064575b600080fd5b348015605857600080fd5b5060626004356088565b005b348015606f57600080fd5b506076608d565b60408051918252519081900360200190f35b600055565b600054905600a165627a7a72305820419b352168794764ac1d5d6d3460eaffedc13c00bcbb4d2ff772148d2f0670fc0029";

    public static final String FUNC_SET = "set";

    public static final String FUNC_GET = "get";

    protected SimpleStorage(final String contractAddress, final Web3j web3j, final Credentials credentials, final ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    protected SimpleStorage(final String contractAddress, final Web3j web3j, final TransactionManager transactionManager, final ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteTransaction<Void> set(final BigInteger x) {
        final Function function = new Function(
                FUNC_SET, 
                Arrays.<Type<?>>asList(new org.web3j.abi.datatypes.generated.Uint256(x)), 
                Collections.<TypeReference<?>>emptyList());
        return new RemoteTransaction0(web3j, function, contractAddress, transactionManager,
                defaultBlockParameter, FunctionEncoder.encode(function), BigInteger.ZERO,
                false, gasProvider);
    }

    public RemoteCall<BigInteger> get() {
        final Function function = new Function(FUNC_GET, 
                Arrays.<Type<?>>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall1<>(function, contractAddress, transactionManager, defaultBlockParameter);
    }

    public static RemoteCall<SimpleStorage> deploy(final Web3j web3j, final Credentials credentials, final ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleStorage.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<SimpleStorage> deploy(final Web3j web3j, final TransactionManager transactionManager, final ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleStorage.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    public static SimpleStorage load(final String contractAddress, final Web3j web3j, final Credentials credentials, final ContractGasProvider contractGasProvider) {
        return new SimpleStorage(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SimpleStorage load(final String contractAddress, final Web3j web3j, final TransactionManager transactionManager, final ContractGasProvider contractGasProvider) {
        return new SimpleStorage(contractAddress, web3j, transactionManager, contractGasProvider);
    }
}
