import mock
import testify as T

from pyleus.storm import StormComponent


class StormComponentTestCase(T.TestCase):

    INSTANCE_CLS = StormComponent

    @T.setup
    def setup(self):
        self.mock_input_stream = mock.Mock()
        self.mock_output_stream = mock.Mock()

        self.instance = self.INSTANCE_CLS(
            input_stream=self.mock_input_stream,
            output_stream=self.mock_output_stream,
        )