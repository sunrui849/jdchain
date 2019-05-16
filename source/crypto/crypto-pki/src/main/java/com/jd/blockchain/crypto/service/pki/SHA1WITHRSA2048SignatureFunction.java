package com.jd.blockchain.crypto.service.pki;

import com.jd.blockchain.crypto.*;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.security.rsa.RSAPrivateCrtKeyImpl;
import sun.security.rsa.RSAPrivateKeyImpl;
import sun.security.rsa.RSAPublicKeyImpl;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static com.jd.blockchain.crypto.BaseCryptoKey.KEY_TYPE_BYTES;
import static com.jd.blockchain.crypto.CryptoBytes.ALGORYTHM_CODE_SIZE;
import static com.jd.blockchain.crypto.CryptoKeyType.PRIVATE;
import static com.jd.blockchain.crypto.CryptoKeyType.PUBLIC;
import static com.jd.blockchain.crypto.service.pki.PKIAlgorithm.SHA1WITHRSA2048;

/**
 * @author zhanglin33
 * @title: SHA1WITHRSA2048SignatureFunction
 * @description: TODO
 * @date 2019-05-15, 16:37
 */
public class SHA1WITHRSA2048SignatureFunction implements SignatureFunction {


    private static final int RAW_PUBKEY_SIZE = 259;
    private static final int RAW_PRIVKEY_SIZE = 1155;

    private static final int RAW_SIGNATUREDIGEST_SIZE = 256;

    private static final AlgorithmIdentifier RSA_ALGORITHM_IDENTIFIER =
            new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);

    @Override
    public SignatureDigest sign(PrivKey privKey, byte[] data) {

        byte[] rawPrivKeyBytes = privKey.getRawKeyBytes();

        if (rawPrivKeyBytes.length < RAW_PRIVKEY_SIZE) {
            throw new CryptoException("This key has wrong format!");
        }

        if (privKey.getAlgorithm() != SHA1WITHRSA2048.code()) {
            throw new CryptoException("This key is not SHA1WITHRSA2048 private key!");
        }

        RSAPrivateCrtKey rawPrivKey;
        Signature signer;
        byte[] signature;
        try {
            rawPrivKey = (RSAPrivateCrtKey) RSAPrivateCrtKeyImpl.newKey(rawPrivKeyBytes);
            signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(rawPrivKey);
            signer.update(data);
            signature = signer.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CryptoException(e.getMessage(), e);
        }

        return new SignatureDigest(SHA1WITHRSA2048, signature);
    }

    @Override
    public boolean verify(SignatureDigest digest, PubKey pubKey, byte[] data) {

        byte[] rawPubKeyBytes = pubKey.getRawKeyBytes();
        byte[] rawDigestBytes = digest.getRawDigest();

        if (rawPubKeyBytes.length < RAW_PUBKEY_SIZE) {
            throw new CryptoException("This key has wrong format!");
        }

        if (pubKey.getAlgorithm() != SHA1WITHRSA2048.code()) {
            throw new CryptoException("This key is not SHA1WITHRSA2048 public key!");
        }

        if (digest.getAlgorithm() != SHA1WITHRSA2048.code() || rawDigestBytes.length != RAW_SIGNATUREDIGEST_SIZE) {
            throw new CryptoException("This is not SHA1WITHRSA2048 signature digest!");
        }
        RSAPublicKeyImpl rawPubKey;
        Signature verifier;
        boolean isValid = false;
        try {
            rawPubKey = new RSAPublicKeyImpl(rawPubKeyBytes);
            verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(rawPubKey);
            verifier.update(data);
            isValid = verifier.verify(rawDigestBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return isValid;
    }

    @Override
    public boolean supportPubKey(byte[] pubKeyBytes) {
        return pubKeyBytes.length > (ALGORYTHM_CODE_SIZE + KEY_TYPE_BYTES + RAW_PUBKEY_SIZE)
                && CryptoAlgorithm.match(SHA1WITHRSA2048, pubKeyBytes)
                && pubKeyBytes[ALGORYTHM_CODE_SIZE] == PUBLIC.CODE;
    }

    @Override
    public PubKey resolvePubKey(byte[] pubKeyBytes) {
        if (supportPubKey(pubKeyBytes)) {
            return new PubKey(pubKeyBytes);
        } else {
            throw new CryptoException("pubKeyBytes are invalid!");
        }
    }

    @Override
    public boolean supportPrivKey(byte[] privKeyBytes) {
        return privKeyBytes.length > (ALGORYTHM_CODE_SIZE + KEY_TYPE_BYTES + RAW_PRIVKEY_SIZE)
                && CryptoAlgorithm.match(SHA1WITHRSA2048, privKeyBytes)
                && privKeyBytes[ALGORYTHM_CODE_SIZE] == PRIVATE.CODE;
    }

    @Override
    public PrivKey resolvePrivKey(byte[] privKeyBytes) {
        if (supportPrivKey(privKeyBytes)) {
            return new PrivKey(privKeyBytes);
        } else {
            throw new CryptoException("privKeyBytes are invalid!");
        }
    }

    @Override
    public PubKey retrievePubKey(PrivKey privKey) {

        byte[] rawPrivKeyBytes = privKey.getRawKeyBytes();

        if (rawPrivKeyBytes.length < RAW_PRIVKEY_SIZE) {
            throw new CryptoException("This key has wrong format!");
        }

        if (privKey.getAlgorithm() != SHA1WITHRSA2048.code()) {
            throw new CryptoException("This key is not SHA1WITHRSA2048 private key!");
        }
        byte[] rawPubKeyBytes;
        try {
            RSAPrivateCrtKey rawPrivKey = (RSAPrivateCrtKey) RSAPrivateCrtKeyImpl.newKey(rawPrivKeyBytes);
            BigInteger modulus =  rawPrivKey.getModulus();
            BigInteger exponent = rawPrivKey.getPublicExponent();
            rawPubKeyBytes = KeyUtil.getEncodedSubjectPublicKeyInfo(RSA_ALGORITHM_IDENTIFIER,
                    new org.bouncycastle.asn1.pkcs.RSAPublicKey(modulus, exponent));
        } catch (InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }

        return new PubKey(SHA1WITHRSA2048, rawPubKeyBytes);
    }

    @Override
    public boolean supportDigest(byte[] digestBytes) {
        return digestBytes.length == (RAW_SIGNATUREDIGEST_SIZE + ALGORYTHM_CODE_SIZE)
                && CryptoAlgorithm.match(SHA1WITHRSA2048, digestBytes);
    }

    @Override
    public SignatureDigest resolveDigest(byte[] digestBytes) {
        if (supportDigest(digestBytes)) {
            return new SignatureDigest(digestBytes);
        } else {
            throw new CryptoException("digestBytes are invalid!");
        }
    }

    @Override
    public AsymmetricKeypair generateKeypair() {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator generator;
        PublicKey  pubKey;
        PrivateKey privKey;
        try {
            generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            pubKey  = keyPair.getPublic();
            privKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new CryptoException(e.getMessage(), e);
        }

        byte[] pubKeyBytes  = pubKey.getEncoded();
        byte[] privKeyBytes = privKey.getEncoded();

        return new AsymmetricKeypair(new PubKey(SHA1WITHRSA2048, pubKeyBytes),
                new PrivKey(SHA1WITHRSA2048, privKeyBytes));
    }

    @Override
    public CryptoAlgorithm getAlgorithm() {
        return SHA1WITHRSA2048;
    }
}
