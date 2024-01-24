// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.signature.internal.testing;

import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.proto.Ed25519PrivateKey;
import com.google.crypto.tink.proto.Ed25519PublicKey;
import com.google.crypto.tink.proto.KeyData;
import com.google.crypto.tink.subtle.Ed25519Verify;
import com.google.crypto.tink.subtle.Hex;
import com.google.crypto.tink.util.Bytes;
import com.google.protobuf.ByteString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class LegacyPublicKeySignKeyManagerTest {
  private static LegacyPublicKeySignKeyManager signKeyManager;
  private static LegacyPublicKeyVerifyKeyManager verifyKeyManager;

  private static byte[] publicKeyByteArray;
  private static byte[] privateKeyByteArray;

  @BeforeClass
  public static void setUp() throws Exception {
    signKeyManager = new LegacyPublicKeySignKeyManager();
    verifyKeyManager = new LegacyPublicKeyVerifyKeyManager();
    publicKeyByteArray =
        Hex.decode("ea42941a6dc801484390b2955bc7376d172eeb72640a54e5b50c95efa2fc6ad8");
    privateKeyByteArray =
        Hex.decode("9cac7d19aeecc563a3dff7bcae0fbbbc28087b986c49a3463077dd5281437e81");
  }

  @Test
  public void testCreatePublicKeySign_works() throws Exception {
    com.google.crypto.tink.signature.Ed25519PublicKey publicKey =
        com.google.crypto.tink.signature.Ed25519PublicKey.create(
            Bytes.copyFrom(publicKeyByteArray));

    Ed25519PublicKey protoPublicKey =
        Ed25519PublicKey.newBuilder()
            .setVersion(0)
            .setKeyValue(ByteString.copyFrom(publicKeyByteArray))
            .build();
    Ed25519PrivateKey protoPrivateKey =
        Ed25519PrivateKey.newBuilder()
            .setVersion(0)
            .setPublicKey(protoPublicKey)
            .setKeyValue(ByteString.copyFrom(privateKeyByteArray))
            .build();
    PublicKeySign signer = signKeyManager.getPrimitive(protoPrivateKey.toByteString());

    PublicKeyVerify verifier = Ed25519Verify.create(publicKey);

    byte[] message = new byte[] {1, 2, 3, 4, 5};
    verifier.verify(signer.sign(message), message);
  }

  @Test
  public void testGetPublicKeyData_works() throws Exception {
    Ed25519PublicKey protoPublicKey =
        Ed25519PublicKey.newBuilder()
            .setVersion(0)
            .setKeyValue(ByteString.copyFrom(publicKeyByteArray))
            .build();
    Ed25519PrivateKey protoPrivateKey =
        Ed25519PrivateKey.newBuilder()
            .setVersion(0)
            .setPublicKey(protoPublicKey)
            .setKeyValue(ByteString.copyFrom(privateKeyByteArray))
            .build();
    PublicKeySign signer = signKeyManager.getPrimitive(protoPrivateKey.toByteString());

    KeyData keyData = signKeyManager.getPublicKeyData(protoPrivateKey.toByteString());
    PublicKeyVerify verifier = verifyKeyManager.getPrimitive(keyData.getValue());

    byte[] message = new byte[] {1, 2, 3, 4, 5};
    verifier.verify(signer.sign(message), message);
  }
}
