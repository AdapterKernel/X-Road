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

require 'test_helper'

# FUTURE: find the way to move test into subproject 'common-ui' where the
# actual production code is
class ScriptUtilsTest < ActiveSupport::TestCase

  EXPECTED_OUTPUT = [
    "Printing each command line argument",
    "-a", "single_word",
    "-b", "with spaces and single quotes",
    "-c", "with spaces and double quotes",
    "-d", "with spaces and escaped double quotes",
    "Using getopts to parse arguments",
    "opt a: single_word",
    "opt b: with spaces and single quotes",
    "opt c: with spaces and double quotes",
    "opt d: with spaces and escaped double quotes"
  ]

  test "Run script with spaces and quotes in the arguments" do
    # Given
    script_file = "#{ENV["XROAD_HOME"]}/"\
        "center-ui/test/resources/echo_script_arguments.sh"

    arguments = ["-a single_word",
                 "-b 'with spaces and single quotes'",
                 '-c "with spaces and double quotes"',
                 "-d \"with spaces and escaped double quotes\""
    ]
    commandline = [script_file] + arguments

    # When
    output = CommonUi::ScriptUtils.run_script(commandline)
    output.collect! { |line| line.strip! }

    # Then
    assert_equal(EXPECTED_OUTPUT, output)
  end
end
