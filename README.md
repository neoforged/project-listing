# Projects Web
A website listing the projects we push to maven.

## Development

The following steps will guide you in setting up this project for local development.

Be aware that queries made to the GitHub API (for repository information) are done without authentication, and are 
subject to [GitHub's rate limiting](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api?apiVersion=2022-11-28).

1. Install [npm](https://docs.npmjs.com/cli/v10/configuring-npm/install).
2. In the project directory, install dependencies through npm:

    ```bash
    npm install
    ```
   
3. Run the development server:
    ```bash
    npm run dev
    ```

4. After making changes and before committing, run ESLint:
    ```bash
    npm run lint
    ```
