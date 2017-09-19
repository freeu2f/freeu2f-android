FreeU2F needs to make some decisions about how to apply crypto. This document
attempts to summarize these choices and give the reasoning behind them. Before
moving further, please see my [summary of U2F][summary] for background
information.

# Crypto Summary

U2F requires two signatures:
1. an attestation signature on the registration reply
2. an authentication signature on the authentication reply

## Attestation Signature

The attestation signature signs the following payload:

| U2F REGISTER Signature Field | Size (Bytes) |
|-----------------------------:|:-------------|
|              Reserved (0x05) | 1            |
|        Application Parameter | 32           |
|          Challenge Parameter | 32           |
|                   Key Handle | 0-255        |
|    Authentication Public Key | 65           |

The important property of the attestation signature is that its certificate
needs to be signed by a certification authority which the server can trust.
In FreeU2F, this comes from the hardware attestation keys.

## Authentication Signature

The authentication signature signs the following payload:

| U2F AUTHENTICATE Signature Field | Size (Bytes) |
|---------------------------------:|:-------------|
|            Application Parameter | 32           |
|              User Presence Flags | 1            |
|                          Counter | 4            |
|              Challenge Parameter | 32           |

The authentication key, if different from the attestation key, does not need to
be signed by a certification authority since its public key is signed by the
attestation key (above).

# Possible Implementations
## Encrypted Private Key in Key Handle

One possible implementation is to have FreeU2F create two global keys on first
launch: an attestation key (P-256) and an encryption key (symmetric). The
global attestation key would be used directly for all attestations.

During the registration step, a new (non-hardware) key pair would be
generated. The newly-generated private key would be encrypted using the global
encryption key and the ciphertext would be returned as the key handle. The
attestation key would sign the newly-generated public key. During the
authentication command, we would decrypt the private key from the key handle.
Using this private key, we would create the authentication signature.

This method basically uses the key handle field as an encrypted cookie.

This implementation method has one big feature: the amount of state in FreeU2F
is constant no matter how many authentication keys are generated. The
corresponding anti-feature is that all authentication signatures are generated
in software rather than in hardware.

## Per-Registration Hardware Keys

Another possible implementation is to generate a new signing key pair in the
hardware key store for each registration. Using this method, the attestation
and authentication keys can be the same; so only one key needs to be generated.

In this case the key handle can be randomly generated. It is, however,
important that the host cannot request a key handle for a different application
parameter. Therefore, we need to somehow link the key handle and the
application parameter such that they can only be used together.

Perhaps the easiest method is to simply make the key store alias for the
signing key the hex encoding of the concatination of:

|        Key Store Alias Element | Size (Bytes) |
|-------------------------------:|:-------------|
|          Application Parameter | 32           |
|              Key Handle Length | 1            |
|                     Key Handle | 0-255        |

This means that we can look up the signing key directly from an offset of the
contents of the authentication message payload. Notice the overlap:

| U2F AUTHENTICATE Request Field | Size (Bytes) |
|-------------------------------:|:-------------|
|            Challenge Parameter | 32           |
|          Application Parameter | 32           |
|              Key Handle Length | 1            |
|                     Key Handle | 0-255        |

This would make it impossible to use a signing key on anything but the same
application parameter and key handle. And we can just randomly generate the key
handle.

This implementation has one big positive: all signing happens inside the
hardware, significantly limiting possible key exposure. This comes at the cost
of a new key in the key store for each registration. The maximum number of keys
in the hardware is unknown (to me).

One might also suggest that a user could, at confirmation for key creation,
enter metadata to uniquely identify the key and then we could have support for
on-device revocation. However, this may not be desirable. Attaching metadata to
keys could leak privacy information. Further, device-side revocation still
doesn't revoke the key on the server. Thus the server-side would still be
vulnerable.

## Trusted Executable Environment

It might be possible to get the best of both worlds by writing a [TEE
process][tee]. However, this would come at the cost of significant additional
complexity. The key store is already implemented as a TEE and might provide us
this functionality in the future.

# Conclusion

FreeU2F is currently implemented with the Per-Registration Hardware Keys model.
This was chosen for its ease of implementation and good security benefits.
However, this might change in the future.

[summary]: https://npmccallum.gitlab.io/post/u2f-protocol-overview/
[tee]: https://source.android.com/security/trusty/
