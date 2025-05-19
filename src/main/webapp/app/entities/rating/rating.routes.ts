import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import RatingResolve from './route/rating-routing-resolve.service';

const ratingRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/rating.component').then(m => m.RatingComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/rating-detail.component').then(m => m.RatingDetailComponent),
    resolve: {
      rating: RatingResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/rating-update.component').then(m => m.RatingUpdateComponent),
    resolve: {
      rating: RatingResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/rating-update.component').then(m => m.RatingUpdateComponent),
    resolve: {
      rating: RatingResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default ratingRoute;
