// Copyright 2023 Google LLC
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

package com.google.crypto.tink.integration.hcvault;

import com.google.crypto.tink.Aead;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.response.LogicalResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link Aead} that forwards encryption/decryption requests to a key in <a
 * href="https://www.vaultproject.io/">Hashicorp Vault</a>.
 *
 * @since 1.0.0
 */
final class HcVaultAead implements Aead {

  /** This client knows how to talk to Hashicorp Vault. */
  private final Logical kmsClient;

  // The location of a crypto key in Hashicorp Vault
  private final String keyUri;

  public HcVaultAead(Logical kmsClient, String keyUri) {
    this.kmsClient = kmsClient;
    this.keyUri = keyUri;
  }

  @Override
  public byte[] encrypt(final byte[] plaintext, final byte[] associatedData)
      throws GeneralSecurityException {
    try {
      String encPath = getOperationEndpoint(this.keyUri, "encrypt");
      Map<String, Object> content = new HashMap<>();
      content.put("plaintext", Base64.getEncoder().encodeToString(plaintext));
      content.put("context", Base64.getEncoder().encodeToString(associatedData));
      LogicalResponse resp = kmsClient.write(encPath, content);
      handleResponse(resp);
      return resp.getData().get("ciphertext").getBytes();
    } catch (VaultException e) {
      throw new GeneralSecurityException("encryption failed", e);
    }
  }

  @Override
  public byte[] decrypt(final byte[] ciphertext, final byte[] associatedData)
      throws GeneralSecurityException {
    try {
      String encPath = getOperationEndpoint(this.keyUri, "decrypt");
      Map<String, Object> content = new HashMap<>();
      content.put("ciphertext", new String(ciphertext));
      content.put("context", Base64.getEncoder().encodeToString(associatedData));
      LogicalResponse resp = kmsClient.write(encPath, content);
      handleResponse(resp);
      return Base64.getDecoder().decode(resp.getData().get("plaintext").getBytes());
    } catch (VaultException e) {
      throw new GeneralSecurityException("decryption failed", e);
    }
  }

  public static String getOperationEndpoint(String keyUri, String operation)
      throws GeneralSecurityException {
    try {
      URI u = new URI(keyUri);
      if (!u.getScheme().equals("hcvault")) {
        throw new GeneralSecurityException("malformed URL");
      }

      String[] parts = u.getPath().split("/");
      if (parts.length < 4 || !parts[parts.length - 2].equals("keys")) {
        throw new GeneralSecurityException(String.format("malformed URL"));
      }

      parts[parts.length - 2] = operation;
      return Arrays.asList(parts).stream().collect(Collectors.joining("/")).replaceFirst("/", "");
    } catch (URISyntaxException e) {
      throw new GeneralSecurityException("malformed URL", e);
    } catch (Exception e) {
      throw new GeneralSecurityException("unknown exception, keyUri: " + keyUri, e);
    }
  }

  /* This shouldn't be necessary, but in older versions of the client library failing requests
   * didn't throw an exception. Belt and braces here to make sure this is thrown.
   */
  private void handleResponse(LogicalResponse resp) throws VaultException {
    int rc = resp.getRestResponse().getStatus();
    if (rc == 200 || rc == 204) {
      return;
    }
    throw new VaultException(
        String.format(
            "Operation failed with HTTP error code %d. Response body: %s",
            rc, new String(resp.getRestResponse().getBody())));
  }
}
