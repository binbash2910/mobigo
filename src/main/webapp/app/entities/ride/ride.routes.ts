import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import RideResolve from './route/ride-routing-resolve.service';

const rideRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/ride.component').then(m => m.RideComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/ride-detail.component').then(m => m.RideDetailComponent),
    resolve: {
      ride: RideResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/ride-update.component').then(m => m.RideUpdateComponent),
    resolve: {
      ride: RideResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/ride-update.component').then(m => m.RideUpdateComponent),
    resolve: {
      ride: RideResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default rideRoute;
