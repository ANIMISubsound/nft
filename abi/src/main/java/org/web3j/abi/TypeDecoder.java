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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.web3j.abi.TypeReference.StaticArrayTypeReference;
import org.web3j.abi.datatypes.AbiTypes;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Array;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Bytes;
import org.web3j.abi.datatypes.BytesType;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Fixed;
import org.web3j.abi.datatypes.FixedPointType;
import org.web3j.abi.datatypes.Int;
import org.web3j.abi.datatypes.IntType;
import org.web3j.abi.datatypes.NumericType;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Ufixed;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.utils.Numeric;

import static org.web3j.abi.TypeReference.makeTypeReference;

/**
 * Ethereum Contract Application Binary Interface (ABI) decoding for types. Decoding is not
 * documented, but is the reverse of the encoding details located <a
 * href="https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI">here</a>.
 */
class TypeDecoder {

    static final int MAX_BYTE_LENGTH_FOR_HEX_STRING = Type.MAX_BYTE_LENGTH << 1;

    public static Type<?> instantiateType(final String solidityType, final Object value)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException,
                    IllegalAccessException, ClassNotFoundException {
        return instantiateType(makeTypeReference(solidityType), value);
    }

    public static <T extends Type<?>> Type<?> instantiateType(
            final TypeReference<T> typeReference, final Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
                    InstantiationException, ClassNotFoundException {
        final Class<? extends Type<?>> refClassType = typeReference.getClassType();
        if (Array.class.isAssignableFrom(refClassType)) {
            return instantiateArrayType(typeReference, value);
        }
        return instantiateAtomicType(refClassType, value);
    }

    @SuppressWarnings("unchecked")
    static <T extends Type<?>> T decode(final String input, final int offset, final Class<T> type) {
        if (NumericType.class.isAssignableFrom(type)) {
            return (T) decodeNumeric(input.substring(offset), (Class<NumericType>) type);
        } else if (Address.class.isAssignableFrom(type)) {
            return (T) decodeAddress(input.substring(offset));
        } else if (Bool.class.isAssignableFrom(type)) {
            return (T) decodeBool(input, offset);
        } else if (Bytes.class.isAssignableFrom(type)) {
            return (T) decodeBytes(input, offset, (Class<Bytes>) type);
        } else if (DynamicBytes.class.isAssignableFrom(type)) {
            return (T) decodeDynamicBytes(input, offset);
        } else if (Utf8String.class.isAssignableFrom(type)) {
            return (T) decodeUtf8String(input, offset);
        } else if (Array.class.isAssignableFrom(type)) {
            throw new UnsupportedOperationException(
                    "Array types must be wrapped in a TypeReference");
        } else {
            throw new UnsupportedOperationException("Type cannot be encoded: " + type);
        }
    }

    static <T extends Type<?>> T decode(final String input, final Class<T> type) {
        return decode(input, 0, type);
    }

    static Address decodeAddress(final String input) {
        return new Address(decodeNumeric(input, Uint160.class));
    }

    static <T extends NumericType> T decodeNumeric(final String input, final Class<T> type) {
        try {
            final byte[] inputByteArray = Numeric.hexStringToByteArray(input);
            final int typeLengthAsBytes = getTypeLengthInBytes(type);

            final byte[] resultByteArray = new byte[typeLengthAsBytes + 1];

            if (Int.class.isAssignableFrom(type) || Fixed.class.isAssignableFrom(type)) {
                resultByteArray[0] = inputByteArray[0]; // take MSB as sign bit
            }

            final int valueOffset = Type.MAX_BYTE_LENGTH - typeLengthAsBytes;
            System.arraycopy(inputByteArray, valueOffset, resultByteArray, 1, typeLengthAsBytes);

            final BigInteger numericValue = new BigInteger(resultByteArray);
            return type.getConstructor(BigInteger.class).newInstance(numericValue);

        } catch (final NoSuchMethodException
                | SecurityException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new UnsupportedOperationException(
                    "Unable to create instance of " + type.getName(), e);
        }
    }

    static <T extends NumericType> int getTypeLengthInBytes(final Class<T> type) {
        return getTypeLength(type) >> 3; // divide by 8
    }

    static <T extends NumericType> int getTypeLength(final Class<T> type) {
        if (IntType.class.isAssignableFrom(type)) {
            final String regex =
                    "(" + Uint.class.getSimpleName() + "|" + Int.class.getSimpleName() + ")";
            final String[] splitName = type.getSimpleName().split(regex);
            if (splitName.length == 2) {
                return Integer.parseInt(splitName[1]);
            }
        } else if (FixedPointType.class.isAssignableFrom(type)) {
            final String regex =
                    "(" + Ufixed.class.getSimpleName() + "|" + Fixed.class.getSimpleName() + ")";
            final String[] splitName = type.getSimpleName().split(regex);
            if (splitName.length == 2) {
                final String[] bitsCounts = splitName[1].split("x");
                return Integer.parseInt(bitsCounts[0]) + Integer.parseInt(bitsCounts[1]);
            }
        }
        return Type.MAX_BIT_LENGTH;
    }

    @SuppressWarnings("unchecked")
    static <T extends Type<?>> T instantiateArrayType(
            final TypeReference<T> typeReference, final Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
                    InstantiationException, ClassNotFoundException {
        final List<?> values;
        if (value instanceof List) {
            values = (List<?>) value;
        } else if (value.getClass().isArray()) {
            values = arrayToList(value);
        } else {
            throw new ClassCastException(
                    "Arg of type "
                            + value.getClass()
                            + " should be a list to instantiate web3j Array");
        }
        final Constructor<T> listConstructor;
        if (typeReference instanceof StaticArrayTypeReference) {
            final StaticArrayTypeReference<?> arrayTypeRef =
                    (StaticArrayTypeReference<?>) typeReference;
            final String className =
                    "org.web3j.abi.datatypes.generated.StaticArray" + arrayTypeRef.getSize();
            final Class<T> arrayClass = (Class<T>) Class.forName(className);
            listConstructor = arrayClass.getConstructor(Class.class, List.class);
        } else {
            final Class<T> arrayClass = typeReference.getClassType();
            listConstructor = arrayClass.getConstructor(Class.class, List.class);
        }
        // create a list of arguments coerced to the correct type of sub-TypeReference
        final List<Type<?>> transformedList = new ArrayList<>(values.size());
        final TypeReference<?> componentTypeRef = typeReference.getComponentTypeReference();
        for (final Object o : values) {
            transformedList.add(instantiateType(componentTypeRef, o));
        }
        return listConstructor.newInstance(componentTypeRef.getClassType(), transformedList);
    }

    @SuppressWarnings("unchecked")
    static <T extends Type<?>> T instantiateAtomicType(
            final Class<T> referenceClass, final Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
                    InstantiationException {
        Object constructorArg = null;
        if (NumericType.class.isAssignableFrom(referenceClass)) {
            constructorArg = asBigInteger(value);
        } else if (BytesType.class.isAssignableFrom(referenceClass)) {
            if (value instanceof byte[]) {
                constructorArg = value;
            } else if (value instanceof BigInteger) {
                constructorArg = ((BigInteger) value).toByteArray();
            } else if (value instanceof String) {
                constructorArg = Numeric.hexStringToByteArray((String) value);
            }
        } else if (Utf8String.class.isAssignableFrom(referenceClass)) {
            constructorArg = value.toString();
        } else if (Address.class.isAssignableFrom(referenceClass)) {
            if (value instanceof BigInteger || value instanceof Uint160) {
                constructorArg = value;
            } else {
                constructorArg = value.toString();
            }
        } else if (Bool.class.isAssignableFrom(referenceClass)) {
            if (value instanceof Boolean) {
                constructorArg = value;
            } else {
                final BigInteger bival = asBigInteger(value);
                constructorArg = bival == null ? null : !bival.equals(BigInteger.ZERO);
            }
        }
        if (constructorArg == null) {
            throw new InstantiationException(
                    "Could not create type "
                            + referenceClass
                            + " from arg "
                            + value.toString()
                            + " of type "
                            + value.getClass());
        }
        final Class<?>[] types = new Class[] {constructorArg.getClass()};
        final Constructor<Type<?>> cons =
                (Constructor<Type<?>>) referenceClass.getConstructor(types);
        return (T) cons.newInstance(constructorArg);
    }

    static <T extends Type<?>> int getSingleElementLength(
            final String input, final int offset, final Class<T> type) {
        if (input.length() == offset) {
            return 0;
        } else if (DynamicBytes.class.isAssignableFrom(type)
                || Utf8String.class.isAssignableFrom(type)) {
            // length field + data value
            return (decodeUintAsInt(input, offset) / Type.MAX_BYTE_LENGTH) + 2;
        } else {
            return 1;
        }
    }

    static int decodeUintAsInt(final String rawInput, final int offset) {
        final String input = rawInput.substring(offset, offset + MAX_BYTE_LENGTH_FOR_HEX_STRING);
        return decode(input, 0, Uint.class).getValue().intValue();
    }

    static Bool decodeBool(final String rawInput, final int offset) {
        final String input = rawInput.substring(offset, offset + MAX_BYTE_LENGTH_FOR_HEX_STRING);
        final BigInteger numericValue = Numeric.toBigInt(input);
        final boolean value = numericValue.equals(BigInteger.ONE);
        return new Bool(value);
    }

    static <T extends Bytes> T decodeBytes(final String input, final Class<T> type) {
        return decodeBytes(input, 0, type);
    }

    static <T extends Bytes> T decodeBytes(
            final String input, final int offset, final Class<T> type) {
        try {
            final String simpleName = type.getSimpleName();
            final String[] splitName = simpleName.split(Bytes.class.getSimpleName());
            final int length = Integer.parseInt(splitName[1]);
            final int hexStringLength = length << 1;

            final byte[] bytes =
                    Numeric.hexStringToByteArray(input.substring(offset, offset + hexStringLength));
            return type.getConstructor(byte[].class).newInstance(bytes);
        } catch (final NoSuchMethodException
                | SecurityException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new UnsupportedOperationException(
                    "Unable to create instance of " + type.getName(), e);
        }
    }

    static DynamicBytes decodeDynamicBytes(final String input, final int offset) {
        final int encodedLength = decodeUintAsInt(input, offset);
        final int hexStringEncodedLength = encodedLength << 1;

        final int valueOffset = offset + MAX_BYTE_LENGTH_FOR_HEX_STRING;

        final String data = input.substring(valueOffset, valueOffset + hexStringEncodedLength);
        final byte[] bytes = Numeric.hexStringToByteArray(data);

        return new DynamicBytes(bytes);
    }

    static Utf8String decodeUtf8String(final String input, final int offset) {
        final DynamicBytes dynamicBytesResult = decodeDynamicBytes(input, offset);
        final byte[] bytes = dynamicBytesResult.getValue();

        return new Utf8String(new String(bytes, StandardCharsets.UTF_8));
    }

    /** Static array length cannot be passed as a type. */
    static <T extends Type<?>> StaticArray<T> decodeStaticArray(
            final String input,
            final int offset,
            final TypeReference<StaticArray<T>> typeReference,
            final int length) {

        final BiFunction<List<T>, String, StaticArray<T>> function =
                (elements, typeName) -> {
                    if (elements.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Zero length fixed array is invalid type");
                    } else {
                        return instantiateStaticArray(
                                (StaticArrayTypeReference<T>) typeReference, elements, length);
                    }
                };

        return decodeArrayElements(input, offset, typeReference, length, function);
    }

    @SuppressWarnings("unchecked")
    static <T extends Type<?>> DynamicArray<T> decodeDynamicArray(
            final String input,
            final int offset,
            final TypeReference<DynamicArray<T>> typeReference) {

        final int length = decodeUintAsInt(input, offset);

        final BiFunction<List<T>, String, DynamicArray<T>> function =
                (elements, typeName) ->
                        new DynamicArray<>((Class<T>) AbiTypes.getType(typeName), elements);

        final int valueOffset = offset + MAX_BYTE_LENGTH_FOR_HEX_STRING;

        return decodeArrayElements(input, valueOffset, typeReference, length, function);
    }

    static BigInteger asBigInteger(final Object arg) {
        if (arg instanceof BigInteger) {
            return (BigInteger) arg;
        } else if (arg instanceof BigDecimal) {
            return ((BigDecimal) arg).toBigInteger();
        } else if (arg instanceof String) {
            return Numeric.toBigInt((String) arg);
        } else if (arg instanceof byte[]) {
            return Numeric.toBigInt((byte[]) arg);
        } else if (arg instanceof Double || arg instanceof Float) {
            return BigDecimal.valueOf(((Number) arg).doubleValue()).toBigInteger();
        } else if (arg instanceof Number) {
            return BigInteger.valueOf(((Number) arg).longValue());
        }
        return null;
    }

    static List<?> arrayToList(final Object array) {
        final int len = java.lang.reflect.Array.getLength(array);
        final ArrayList<Object> result = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            result.add(java.lang.reflect.Array.get(array, i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Type<?>> StaticArray<T> instantiateStaticArray(
            final StaticArrayTypeReference<T> typeReference,
            final List<T> elements,
            final int length) {
        try {
            final Class<? extends StaticArray<Type<?>>> arrayClass =
                    (Class<? extends StaticArray<Type<?>>>)
                            Class.forName("org.web3j.abi.datatypes.generated.StaticArray" + length);

            return (StaticArray<T>)
                    arrayClass
                            .getConstructor(Class.class, List.class)
                            .newInstance(typeReference.getComponentType(), elements);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <A extends Array<T>, T extends Type<?>> A decodeArrayElements(
            final String input,
            final int offset,
            final TypeReference<A> typeReference,
            final int length,
            final BiFunction<List<T>, String, A> consumer) {

        try {
            final Class<T> cls = Utils.getParameterizedTypeFromArray(typeReference);
            if (Array.class.isAssignableFrom(cls)) {
                throw new IllegalArgumentException(
                        "Arrays of arrays are not currently supported for external functions, see"
                                + "http://solidity.readthedocs.io/en/develop/types.html#members");
            } else {
                final List<T> elements = new ArrayList<>(length);

                for (int i = 0, currOffset = offset;
                        i < length;
                        i++,
                                currOffset +=
                                        getSingleElementLength(input, currOffset, cls)
                                                * MAX_BYTE_LENGTH_FOR_HEX_STRING) {
                    final T value = decode(input, currOffset, cls);
                    elements.add(value);
                }

                final String typeName = Utils.getSimpleTypeName(cls);
                return consumer.apply(elements, typeName);
            }
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Unable to access parameterized type " + typeReference.getType().getTypeName(),
                    e);
        }
    }
}
