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

# Base class for configuration generators
class ConfGenerator

  # return configuration version e.g. 1, 2...
  def getVersion
    raise NotImplementedError.new("Not implemented")
  end

  # tells whether this configuration is the current one
  def isCurrentVersion?
    raise NotImplementedError.new("Not implemented")
  end

  # generate private parameters
  def generatePrivateParameters
    raise NotImplementedError.new("Not implemented")
  end

  # validate private parameters
  def validatePrivateParameters(private_parameters_xml)
    raise NotImplementedError.new("Not implemented")
  end

  # generate shared parameters
  def generateSharedParameters
    raise NotImplementedError.new("Not implemented")
  end

  # validate shared parameters
  def validateSharedParameters(shared_parameters_xml)
    raise NotImplementedError.new("Not implemented")
  end

end
