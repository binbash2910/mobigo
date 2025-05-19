import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import MessageResolve from './route/message-routing-resolve.service';

const messageRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/message.component').then(m => m.MessageComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/message-detail.component').then(m => m.MessageDetailComponent),
    resolve: {
      message: MessageResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/message-update.component').then(m => m.MessageUpdateComponent),
    resolve: {
      message: MessageResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/message-update.component').then(m => m.MessageUpdateComponent),
    resolve: {
      message: MessageResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default messageRoute;
