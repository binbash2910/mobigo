import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import StepResolve from './route/step-routing-resolve.service';

const stepRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/step.component').then(m => m.StepComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/step-detail.component').then(m => m.StepDetailComponent),
    resolve: {
      step: StepResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/step-update.component').then(m => m.StepUpdateComponent),
    resolve: {
      step: StepResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/step-update.component').then(m => m.StepUpdateComponent),
    resolve: {
      step: StepResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default stepRoute;
