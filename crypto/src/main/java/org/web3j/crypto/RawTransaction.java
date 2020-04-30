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
package org.web3j.crypto;

import java.math.BigInteger;

import org.web3j.utils.Numeric;

/**
 * Transaction class used for signing transactions locally.<br>
 * For the specification, refer to p4 of the <a href="http://gavwood.com/paper.pdf">yellow
 * paper</a>.
 */
public class RawTransaction {

    private final BigInteger nonce;
    private final BigInteger gasPrice;
    private final BigInteger gasLimit;
    private final String to;
    private final BigInteger value;
    private final String data;
    private final BigInteger gasPremium;
    private final BigInteger feeCap;

    protected RawTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final String to,
            final BigInteger value,
            final String data) {
        this(nonce, gasPrice, gasLimit, to, value, data, null, null);
    }

    protected RawTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final String to,
            final BigInteger value,
            final String data,
            final BigInteger gasPremium,
            final BigInteger feeCap) {
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.data = data != null ? Numeric.cleanHexPrefix(data) : null;
        this.gasPremium = gasPremium;
        this.feeCap = feeCap;
    }

    public static RawTransaction createContractTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final BigInteger value,
            final String init) {

        return new RawTransaction(nonce, gasPrice, gasLimit, "", value, init);
    }

    public static RawTransaction createEtherTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final String to,
            final BigInteger value) {

        return new RawTransaction(nonce, gasPrice, gasLimit, to, value, "");
    }

    public static RawTransaction createEtherTransaction(
            final BigInteger nonce,
            final BigInteger gasLimit,
            final String to,
            final BigInteger value,
            final BigInteger gasPremium,
            final BigInteger feeCap) {
        return new RawTransaction(nonce, null, gasLimit, to, value, "", gasPremium, feeCap);
    }

    public static RawTransaction createTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final String to,
            final String data) {
        return createTransaction(nonce, gasPrice, gasLimit, to, BigInteger.ZERO, data);
    }

    public static RawTransaction createTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final String to,
            final BigInteger value,
            final String data) {

        return new RawTransaction(nonce, gasPrice, gasLimit, to, value, data);
    }

    public static RawTransaction createTransaction(
            final BigInteger nonce,
            final BigInteger gasPrice,
            final BigInteger gasLimit,
            final String to,
            final BigInteger value,
            final String data,
            final BigInteger gasPremium,
            final BigInteger feeCap) {

        return new RawTransaction(nonce, gasPrice, gasLimit, to, value, data, gasPremium, feeCap);
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getValue() {
        return value;
    }

    public String getData() {
        return data;
    }

    public BigInteger getGasPremium() {
        return gasPremium;
    }

    public BigInteger getFeeCap() {
        return feeCap;
    }

    public boolean isLegacyTransaction() {
        return gasPrice != null && gasPremium == null && feeCap == null;
    }

    public boolean isEIP1559Transaction() {
        return gasPrice == null && gasPremium != null && feeCap != null;
    }
}
