#
# The MIT License
# Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

java_import Java::ee.ria.xroad.common.util.CryptoUtils

class CertValidator
  CERT_MAX_BYTES = 1000000 # 1 MB

  def validate(cert_file, original_filename)
    cert_size = File.size(cert_file)

    if cert_size > CERT_MAX_BYTES
      CommonUi::CertUtils.raise_invalid_cert
    end

    raw_cert = IO.read(cert_file)
    java_cert = CryptoUtils::readCertificate(raw_cert.to_java_bytes())

    validate_specific(java_cert)
  rescue Java::java.lang.Exception
    CommonUi::CertUtils.raise_invalid_cert
  end

  private

  def validate_specific(java_cert)
    # Do nothing for general cert.
  end
end
