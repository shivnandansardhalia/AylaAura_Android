module Fastlane
  module Actions
    class GitCommandAction < Action
      def self.run(params)
        Actions.sh(params[:full_command])
      end

      def self.description
        "Executes a given generic git command"
      end

      def self.available_options
        [
          FastlaneCore::ConfigItem.new(key: :full_command,
                                       description: "the specific full git command to execute",
                                       is_string: true,
                                       optional: false,
                                       ),
        ]
      end

      def self.authors
        ["AylaGene"]
      end

      def self.is_supported?(platform)
        true
      end
    end
  end
end
