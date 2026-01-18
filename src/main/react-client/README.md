# Mobigo React API Client

Generated TypeScript/Axios client for the Mobigo API.

## Prerequisites

The backend server must be running on `http://localhost:8080` to generate the client.

## Generate the client

From the project root:

```bash
./npmw run generate:react-client
```

Or from this directory:

```bash
npm install
npm run build
```

## Generated files

The generated client will be in the `generated/` folder containing:

- **api.ts** - API client classes with all endpoints
- **base.ts** - Base configuration
- **common.ts** - Common utilities
- **configuration.ts** - API configuration class
- **models/** or inline interfaces - TypeScript interfaces for all DTOs

## Usage in React

```typescript
import { Configuration, PeopleApiApi, RideApiApi } from './generated';

const config = new Configuration({
  basePath: 'http://localhost:8080',
  accessToken: 'your-jwt-token',
});

const peopleApi = new PeopleApiApi(config);
const rideApi = new RideApiApi(config);

// Example: Get all rides
const rides = await rideApi.getAllRides();
```
