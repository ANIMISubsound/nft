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
package org.web3j.abi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.StaticArray2;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctionReturnDecoderTest {

    @Test
    public void testSimpleFunctionDecode() {
        final Function function =
                new Function(
                        "test",
                        Collections.<Type<?>>emptyList(),
                        Collections.singletonList(new TypeReference<Uint>() {}));

        assertEquals(
                FunctionReturnDecoder.decode(
                        "0x0000000000000000000000000000000000000000000000000000000000000037",
                        function.getOutputParameters()),
                (Collections.singletonList(new Uint(BigInteger.valueOf(55)))));
    }

    @Test
    public void testSimpleFunctionStringResultDecode() {
        final Function function =
                new Function(
                        "simple",
                        Arrays.asList(),
                        Collections.singletonList(new TypeReference<Utf8String>() {}));

        final List<Type<?>> utf8Strings =
                FunctionReturnDecoder.decode(
                        "0x0000000000000000000000000000000000000000000000000000000000000020"
                                + "000000000000000000000000000000000000000000000000000000000000000d"
                                + "6f6e65206d6f72652074696d6500000000000000000000000000000000000000",
                        function.getOutputParameters());

        assertEquals(utf8Strings.get(0).getValue(), ("one more time"));
    }

    @Test
    public void testFunctionEmptyStringResultDecode() {
        final Function function =
                new Function(
                        "test",
                        Collections.emptyList(),
                        Collections.singletonList(new TypeReference<Utf8String>() {}));

        final List<Type<?>> utf8Strings =
                FunctionReturnDecoder.decode(
                        "0x0000000000000000000000000000000000000000000000000000000000000020"
                                + "0000000000000000000000000000000000000000000000000000000000000000",
                        function.getOutputParameters());

        assertEquals(utf8Strings.get(0).getValue(), (""));
    }

    @Test
    public void testMultipleResultFunctionDecode() {
        final Function function =
                new Function(
                        "test",
                        Collections.<Type<?>>emptyList(),
                        Arrays.asList(new TypeReference<Uint>() {}, new TypeReference<Uint>() {}));

        assertEquals(
                FunctionReturnDecoder.decode(
                        "0x0000000000000000000000000000000000000000000000000000000000000037"
                                + "0000000000000000000000000000000000000000000000000000000000000007",
                        function.getOutputParameters()),
                (Arrays.asList(new Uint(BigInteger.valueOf(55)), new Uint(BigInteger.valueOf(7)))));
    }

    @Test
    public void testDecodeMultipleStringValues() {
        final Function function =
                new Function(
                        "function",
                        Collections.<Type<?>>emptyList(),
                        Arrays.asList(
                                new TypeReference<Utf8String>() {},
                                        new TypeReference<Utf8String>() {},
                                new TypeReference<Utf8String>() {},
                                        new TypeReference<Utf8String>() {}));

        assertEquals(
                FunctionReturnDecoder.decode(
                        "0x0000000000000000000000000000000000000000000000000000000000000080"
                                + "00000000000000000000000000000000000000000000000000000000000000c0"
                                + "0000000000000000000000000000000000000000000000000000000000000100"
                                + "0000000000000000000000000000000000000000000000000000000000000140"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "6465663100000000000000000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "6768693100000000000000000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "6a6b6c3100000000000000000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "6d6e6f3200000000000000000000000000000000000000000000000000000000",
                        function.getOutputParameters()),
                (Arrays.asList(
                        new Utf8String("def1"), new Utf8String("ghi1"),
                        new Utf8String("jkl1"), new Utf8String("mno2"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDecodeStaticArrayValue() {
        final List<TypeReference<Type<?>>> outputParameters = new ArrayList<>(1);
        outputParameters.add(
                (TypeReference)
                        new TypeReference.StaticArrayTypeReference<StaticArray<Uint256>>(2) {});
        outputParameters.add((TypeReference) new TypeReference<Uint256>() {});

        final List<Type<?>> decoded =
                FunctionReturnDecoder.decode(
                        "0x0000000000000000000000000000000000000000000000000000000000000037"
                                + "0000000000000000000000000000000000000000000000000000000000000001"
                                + "000000000000000000000000000000000000000000000000000000000000000a",
                        outputParameters);

        final StaticArray2<Uint256> uint256StaticArray2 =
                new StaticArray2<>(
                        Uint256.class,
                        new Uint256(BigInteger.valueOf(55)),
                        new Uint256(BigInteger.ONE));

        final List<Type<?>> expected =
                Arrays.asList(uint256StaticArray2, new Uint256(BigInteger.TEN));
        assertEquals(decoded, (expected));
    }

    @Test
    public void testVoidResultFunctionDecode() {
        final Function function =
                new Function("test", Collections.emptyList(), Collections.emptyList());

        assertEquals(
                FunctionReturnDecoder.decode("0x", function.getOutputParameters()),
                (Collections.emptyList()));
    }

    @Test
    public void testEmptyResultFunctionDecode() {
        final Function function =
                new Function(
                        "test",
                        Collections.emptyList(),
                        Collections.singletonList(new TypeReference<Uint>() {}));

        assertEquals(
                FunctionReturnDecoder.decode("0x", function.getOutputParameters()),
                (Collections.emptyList()));
    }

    @Test
    public void testDecodeIndexedUint256Value() {
        final Uint256 value = new Uint256(BigInteger.TEN);
        final String encoded = TypeEncoder.encodeNumeric(value);

        assertEquals(
                FunctionReturnDecoder.decodeIndexedValue(encoded, new TypeReference<Uint256>() {}),
                (value));
    }

    @Test
    public void testDecodeIndexedStringValue() {
        final Utf8String string = new Utf8String("some text");
        final String encoded = TypeEncoder.encodeString(string);
        final String hash = Hash.sha3(encoded);

        assertEquals(
                FunctionReturnDecoder.decodeIndexedValue(hash, new TypeReference<Utf8String>() {}),
                (new Bytes32(Numeric.hexStringToByteArray(hash))));
    }

    @Test
    public void testDecodeIndexedBytes32Value() {
        final String rawInput =
                "0x1234567890123456789012345678901234567890123456789012345678901234";
        final byte[] rawInputBytes = Numeric.hexStringToByteArray(rawInput);

        assertEquals(
                FunctionReturnDecoder.decodeIndexedValue(rawInput, new TypeReference<Bytes32>() {}),
                (new Bytes32(rawInputBytes)));
    }

    @Test
    public void testDecodeIndexedBytes16Value() {
        final String rawInput =
                "0x1234567890123456789012345678901200000000000000000000000000000000";
        final byte[] rawInputBytes = Numeric.hexStringToByteArray(rawInput.substring(0, 34));

        assertEquals(
                FunctionReturnDecoder.decodeIndexedValue(rawInput, new TypeReference<Bytes16>() {}),
                (new Bytes16(rawInputBytes)));
    }

    @Test
    public void testDecodeIndexedDynamicBytesValue() {
        final DynamicBytes bytes = new DynamicBytes(new byte[] {1, 2, 3, 4, 5});
        final String encoded = TypeEncoder.encodeDynamicBytes(bytes);
        final String hash = Hash.sha3(encoded);

        assertEquals(
                FunctionReturnDecoder.decodeIndexedValue(
                        hash, new TypeReference<DynamicBytes>() {}),
                (new Bytes32(Numeric.hexStringToByteArray(hash))));
    }

    @Test
    public void testDecodeIndexedDynamicArrayValue() {
        final DynamicArray<Uint256> array =
                new DynamicArray<>(Uint256.class, new Uint256(BigInteger.TEN));

        final String encoded = TypeEncoder.encodeDynamicArray(array);
        final String hash = Hash.sha3(encoded);

        assertEquals(
                FunctionReturnDecoder.decodeIndexedValue(
                        hash, new TypeReference<DynamicArray<Type<?>>>() {}),
                (new Bytes32(Numeric.hexStringToByteArray(hash))));
    }
}
